# 수강 정원 검증과 동시성 제어

수강 신청 API에 강의별 최대 수강 인원(정원) 검증을 추가하면서 발생한 동시성 문제와,
비관적 락 기반으로 해결한 과정을 정리한다.

## 1. 기능 요약

- `lectures` 테이블에 `max_enrollment INT NOT NULL` (CHECK `> 0`) 컬럼 추가
- lecture-service 내부 API(`GET /api/v1/lectures/{id}`)가 `maxEnrollment`를 응답에 포함
- enrollment-service의 `POST /enrollments`가 현재 수강 인원과 정원을 비교해
  초과 시 `409 ENROLLMENT_CAPACITY_EXCEEDED("수강 정원이 초과된 강의입니다.")` 반환

수강 신청 검증 규칙 (순서대로):

1. 활성 회원인가 — member-service REST 호출
2. 존재하는 PUBLIC 강의인가 — lecture-service REST 호출
3. 중복 신청이 아닌가 — `enrollments` UNIQUE(member_id, lecture_id)
4. **정원이 남아 있는가 — `countByLectureId` vs `maxEnrollment`**

## 2. 동시성 문제: count-then-insert 레이스

### 문제

정원 검증이 "count 조회 → insert" 두 단계로 이루어지는데, 두 단계 사이에
다른 트랜잭션의 **미커밋 insert는 count에 보이지 않는다** (READ COMMITTED /
REPEATABLE READ 모두 동일). 따라서 동시에 들어온 요청들이 전부
"현재 0명 < 정원 1명" 검증을 통과한 뒤 각자 insert를 커밋할 수 있다.

중복 신청은 UNIQUE 제약이 DB 레벨에서 막아주지만, 정원은 그런 장치가 없다.

### 재현 결과 (수정 전)

정원 1명짜리 강의에 서로 다른 활성 회원 20명이 동시에 신청:

| 항목 | 결과 |
|---|---|
| HTTP 201 (성공) | **8건** |
| HTTP 409 (정원 초과) | 12건 |
| DB 실제 수강 인원 | **8명 / 정원 1명** |

8건 모두 `enrolled_at`이 같은 초에 몰려 있었다 — 8개 트랜잭션이 전부 count=0을
보고 통과한 것이다. 뒤늦게 도착한 12건만 커밋된 8건이 보여서 차단됐다.

## 3. 해결 방법 비교

| 방법 | 개요 | 평가 |
|---|---|---|
| `lectures` 행 비관적 락 | enrollment가 `lectures` 행을 `FOR UPDATE` | 지금은 DB를 공유해서 동작하지만, 남의 서비스 테이블에 락을 거는 것은 MSA 경계 침범. DB 분리 시 불가능 |
| **`lecture_seats` 잠금 행 (채택)** | enrollment 소유의 강의별 잠금 행을 `FOR UPDATE` | 자기 테이블만 사용, DB 분리에도 유효, 강의 단위로만 직렬화 |
| 잔여 좌석 카운터 | `UPDATE ... SET remaining = remaining - 1 WHERE remaining > 0` 원자 차감 | 유효한 대안. 정원 변경 동기화가 추가로 필요 |
| 분산 락 (Redis 등) | 서비스 스케일아웃 + DB 분리 환경의 일반해 | 현 규모에는 과함 |

### 왜 `lectures` 행을 직접 잠그지 않는가

- **경계 침범**: `lectures`의 소유자는 lecture-service다. 강의 정보를 REST 내부
  API로 조회하는 이유가 이 경계를 지키기 위해서인데, 락만 예외로 둘 수 없다.
- **장애 전파**: enrollment의 트랜잭션이 `lectures` 행 락을 오래 잡으면
  lecture-service의 강의 수정이 영문도 모른 채 대기한다.
- **미래 비호환**: DB를 서비스별로 분리하는 순간 물리적으로 불가능해진다.
  자기 소유 테이블 락은 DB가 분리되어도 그대로 유효하다.

## 4. 채택한 설계: `lecture_seats` 잠금 행 + 비관적 락

### 테이블

```sql
CREATE TABLE lecture_seats (
    lecture_id BIGINT NOT NULL,
    PRIMARY KEY (lecture_id)
);
```

데이터가 아니라 **강의별 뮤텍스** 역할이다. 이 행을 `SELECT ... FOR UPDATE`로
잠그면 같은 강의의 수강 신청 트랜잭션들이 그 지점에서 한 줄로 직렬화된다.
시드 강의는 `data.sql`에서 미리 생성하고, 이후 생기는 강의는 첫 신청 시
`INSERT IGNORE`로 만들어진다(upsert — 동시에 두 건이 만들어도 PK 충돌로 한 건만 삽입).

### 동작 흐름

```
트랜잭션 A                                  트랜잭션 B
──────────                                 ──────────
[트랜잭션 밖] REST 검증, 잠금 행 확보         [트랜잭션 밖] REST 검증, 잠금 행 확보
BEGIN                                      BEGIN
lecture_seats FOR UPDATE  ← 락 획득
                                           lecture_seats FOR UPDATE  ← 대기
COUNT(*) → 0건, 정원 통과
INSERT enrollments
COMMIT                    ← 락 해제
                                           ← 락 획득
                                           COUNT(*) → 1건 (A의 커밋이 보임)
                                           → 정원 초과 409, ROLLBACK
```

count를 하려면 먼저 락을 잡아야 하고, 락은 앞 트랜잭션이 커밋해야 풀리므로
count가 항상 커밋된 최신 값을 보게 된다. 레이스 창이 원천적으로 닫힌다.

## 5. 구현 중 만난 함정 2가지

단순히 "트랜잭션 안에서 INSERT IGNORE → FOR UPDATE"로 구현하면 두 가지
장애가 순차적으로 발생한다. 실제로 둘 다 재현했다.

### 함정 1 — INSERT IGNORE의 공유 락(S) → FOR UPDATE 데드락

**증상**: 20건 동시 요청 시 정원은 지켜졌지만(1명) 13건이 409 대신 500.
로그: `Deadlock found when trying to get lock`

**원인**: `INSERT IGNORE`는 중복 키 체크 과정에서 해당 인덱스 레코드에
**공유 락(S)을 잡고, 이 락은 트랜잭션 끝까지 유지**된다. 여러 트랜잭션이
S락을 쥔 채 각자 `FOR UPDATE`(X락)를 요청하면 서로의 S락 해제를 기다리며
교착한다. MySQL이 데드락을 감지해 희생 트랜잭션을 롤백 →
`CannotAcquireLockException` → 500.

**해결**: 잠금 행 생성(`INSERT IGNORE`)을 임계 구역 트랜잭션과 **분리된
독립 트랜잭션**에서 실행하고 즉시 커밋해 S락을 바로 해제한다
(`LectureSeatInitializer`, `REQUIRES_NEW`).

### 함정 2 — REQUIRES_NEW의 커넥션 풀 고갈 데드락

**증상**: 함정 1 수정 직후 20건 동시 요청이 **전부 500**, 30초 타임아웃.
로그: `HikariPool-1 - Connection is not available ... (total=10, active=10, idle=0, waiting=10)`

**원인**: `enroll()` 전체가 `@Transactional`인 상태에서 내부의 REQUIRES_NEW는
**두 번째 커넥션**을 요구한다. 동시 요청들이 바깥 트랜잭션 커넥션으로 풀(10개)을
전부 점유한 채 각자 두 번째 커넥션을 기다리니, 아무도 진행하지 못하는
풀 레벨 교착이 발생했다.

**해결**: `enroll()`에서 `@Transactional`을 제거하고 구조를 재편했다.
임계 구역은 별도 빈(`EnrollmentProcessor`)의 `@Transactional` 메서드로 분리했다
— 같은 빈 안에서 자기 메서드를 호출하면 프록시를 타지 않아 선언적
트랜잭션이 적용되지 않으므로, 빈을 나누어야 한다.

```
EnrollmentService.enroll()          ← 트랜잭션 아님 (커넥션 비점유)
├─ 회원/강의 REST 검증               ← 락·트랜잭션 밖 (락 보유 시간 최소화)
├─ 잠금 행 확보 (없을 때만)           ← LectureSeatInitializer의 독립 트랜잭션, 즉시 커밋
└─ EnrollmentProcessor.enroll()     ← 임계 구역만 @Transactional
   ├─ lecture_seats FOR UPDATE      ← 여기서 강의별 직렬화
   ├─ 중복 신청 검사
   ├─ 정원 검증 (count vs max)
   └─ enrollments INSERT → COMMIT (락 해제)
```

핵심 원칙 두 가지:

1. **외부 REST 호출은 락/트랜잭션 밖에서 끝낸다.** 락을 쥔 채 다른 서비스를
   호출하면 그 서비스의 지연이 락 보유 시간으로 전이된다.
2. **커넥션을 쥔 채 또 다른 커넥션을 기다리지 않는다.** 중첩 트랜잭션이
   필요하면 바깥이 커넥션을 점유하지 않는 구조로 만든다.

## 6. 최종 검증 결과

정원 1명 강의 + 활성 회원 20명 동시 신청 (잠금 행도 없는 콜드 상태에서 시작):

| 항목 | 수정 전 | 수정 후 |
|---|---|---|
| HTTP 201 | 8건 | **1건** |
| HTTP 409 (정원 초과) | 12건 | **19건** |
| HTTP 500 | 0건 | **0건** |
| DB 실제 수강 인원 | 8명 / 정원 1 | **1명 / 정원 1** |
| 데드락/풀 고갈 로그 | 발생 | **0건** |

회귀 확인: 여유 있는 강의 정상 신청 201, 중복 신청 409, 정원 마감 강의 409 모두 정상.

## 7. 관련 코드

| 파일 | 역할 |
|---|---|
| `db/schema.sql` | `lectures.max_enrollment`, `lecture_seats` 테이블 |
| `lecture-service/.../lecture/domain/Lecture.java` | `maxEnrollment` 필드 |
| `lecture-service/.../lecture/dto/LectureResponse.java` | 내부 API 응답에 `maxEnrollment` 포함 |
| `enrollment-service/.../enrollment/domain/LectureSeat.java` | 잠금 행 엔티티 |
| `enrollment-service/.../enrollment/repository/LectureSeatRepository.java` | `findWithLock`(PESSIMISTIC_WRITE), `insertIgnore` |
| `enrollment-service/.../enrollment/service/LectureSeatInitializer.java` | 잠금 행 생성 전용 독립 트랜잭션 |
| `enrollment-service/.../enrollment/service/EnrollmentService.java` | 비트랜잭션 오케스트레이션 (REST 검증, 잠금 행 확보) |
| `enrollment-service/.../enrollment/service/EnrollmentProcessor.java` | 임계 구역 `@Transactional` (락 → 중복·정원 검증 → 저장) |

## 8. 한계와 확장 지점

- **강의 단위 직렬화 비용**: 같은 강의의 신청은 한 줄로 처리된다. 인기 강의
  오픈런에서는 락 대기가 생기지만, 이는 정원제의 본질적 비용이다. 서로 다른
  강의끼리는 병렬 처리된다.
- **락 대기 중 커넥션 점유**: 대기자는 커넥션을 쥔 채 행 락을 기다린다.
  극단적 트래픽에서는 락 타임아웃(`jakarta.persistence.lock.timeout`) 설정과
  대기열(Redis 등) 도입을 검토한다.
- **DB 공유 전제**: 현재는 enrollment-service 인스턴스를 여러 개 띄워도 같은
  DB를 쓰는 한 유효하다. DB까지 분리·샤딩되면 분산 락 또는 이벤트 기반
  좌석 예약 패턴으로 확장해야 한다.
