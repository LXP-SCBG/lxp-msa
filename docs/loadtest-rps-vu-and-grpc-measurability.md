# 부하 테스트 방법론 — RPS/VU 설정과 "HTTP→gRPC 성능 개선"을 이 환경에서 측정할 수 있는가

수강 신청(enrollment) 부하 테스트를 하며 나온 두 가지 논의를 정리한다.

1. 테스트하는 **컴퓨터 사양에 따라 RPS·VU를 어떻게 잡아야 하는가**
2. 내부 API를 **HTTP → gRPC 로 바꿨을 때 RPS가 향상되는지**를 **지금 Docker 환경에서 신뢰성 있게 측정할 수 있는가**

---

## 0. 측정 환경 (전제)

이 결론들은 아래 환경을 전제로 한다. 환경이 바뀌면 숫자와 판단도 달라진다.

| 항목 | 값 |
|---|---|
| 호스트 | 8 코어 / 16 GB (macOS) |
| Docker Desktop VM | **4 vCPU / 8 GB** (컨테이너가 실제로 쓸 수 있는 자원) |
| 돌아가는 컨테이너 | ~14개 — mysql, consul, gateway, member/lecture/goal, **enrollment ×3**, prometheus, loki, alloy, grafana, front |
| 부하 생성기 | k6 (호스트에서 **네이티브 실행** → Docker VM과 **같은 물리 코어**를 나눠 씀) |
| 부하 경로 | k6 → gateway(:7070) → enrollment ×3 → member + lecture → mysql |

**핵심 제약**: 측정 대상(SUT)과 부하 생성기(k6), 그리고 관측 스택이 **한 대의 4-vCPU 머신에 모두 co-located** 되어 있다. 이 한 줄이 아래 모든 판단의 근거다.

---

## 1. 컴퓨터 사양에 따른 RPS / VU 설정

### 1-1. 개념 — RPS와 VU는 서로 다른 것

- **RPS (arrival rate)** = 내가 **주입하는 부하량**. `ramping-arrival-rate` executor로 직접 제어한다.
- **VU (virtual user)** = 그 RPS를 **유지하는 데 필요한 동시성 풀**. 목표가 아니라 **리틀의 법칙**으로 따라오는 값이다.

```
필요 VU ≈ 목표 RPS × 평균 응답시간(초)
```

> 예) 목표 600 RPS인데 응답 p95가 1.5초로 늘어나면 → 순간 필요 VU ≈ 900.
> 그래서 `maxVUs`는 이 추정치보다 **넉넉한 상한(풀)** 으로 둔다. 정원(1000)에 맞춰 낮추면,
> 응답이 느려질 때 k6가 목표 RPS를 못 쏴서 `dropped_iterations`가 발생하고, 결과가
> "서버 성능"이 아니라 "부하 생성기 한계"를 재게 되어 오염된다.

### 1-2. 사양은 "목표값"이 아니라 "천장"이다

흔한 오해: "이 컴퓨터는 N코어니까 RPS를 N으로 맞춘다."
→ **틀렸다.** 사양이 정하는 것은 목표 RPS가 아니라 **믿을 수 있는 RPS의 상한**이다.

올바른 방법은 **계단식으로 램프업하며 무릎점(knee, 포화 지점)을 찾는 것**이다:

- 각 단계에서 유지 구간을 두고 관찰한다 (지금 스크립트가 이미 이렇게 함: 50→100→200→400→600).
- 어느 단계부터 ① 처리량이 목표를 못 따라가고 ② p95 지연이 급등하고 ③ VU가 폭증하는가.
- **그 지점 + 한 단계 정도**까지만 밀면 충분하다. 그 이상은 "생성기/CPU 한계"를 재는 것.

### 1-3. 이 머신에서의 현실적 숫자

가장 큰 제약은 **Docker VM이 4 vCPU** 라는 점이다. 부하 경로만 해도 JVM(gateway + enrollment×3 + member + lecture) + MySQL이 4코어를 두고 경합한다.

| 항목 | 권장 | 이유 |
|---|---|---|
| 목표 RPS | 램프로 **무릎점 탐색** (이 박스는 대략 수백 RPS대) | 4 vCPU + 요청당 DB 3~4회가 천장. 3000 RPS는 무의미 |
| maxVUs | 2000 유지 | 리틀의 법칙 + 지연 상승 대비 풀. 낮추면 dropped_iterations 오염 |
| preAllocatedVUs | 500 | 초기 램프에 필요한 만큼 미리 확보 |
| 유효성 판단 | `dropped_iterations = 0`, 호스트 CPU < 100% | 이게 깨지면 그 구간 숫자는 서버 한계가 아님 |

> 스크립트 헤더 주석에 "3000 RPS / spike" 라고 적혀 있지만, **4코어 co-located 환경에서 3000은
> 서버가 아니라 CPU 기아·부하 생성기 한계를 측정하게 된다.** 현재 램프(→600)가 이 사양에 적절하다.

### 1-4. 결과 신뢰도 — co-located의 한계

생성기와 SUT가 CPU를 나눠 쓰면 **절대 수치("이 시스템은 X RPS 처리")는 신뢰할 수 없다.** 얻을 수 있는 것:

| 얻을 수 있는 것 | 신뢰도 |
|---|---|
| 정합성 검증 (오버셀 0, 정확히 정원만큼만 INSERT) | ✅ 신뢰 가능 |
| 상대 비교 (1대 vs 3대, 락 전/후) — **CPU 미포화 구간에서만** | ⚠️ 조건부 |
| 절대 처리량 벤치마크 | ❌ 불가 |

신뢰도를 높이는 순서:

1. **k6를 별도 머신에서** 실행 (가장 확실)
2. Docker Desktop CPU를 6개로 상향(호스트 8코어 중), 호스트에 2코어를 k6 몫으로 남김. 부하 중 호스트 CPU가 100%면 그 데이터는 버린다.
3. **부하 중 관측 스택 끄기**: `docker compose stop prometheus loki alloy grafana front` — 4코어에서 이들이 CPU를 훔쳐 결과를 오염시킨다.
4. 부하 경로 컨테이너에 `deploy.resources.limits.cpus`로 우선순위 부여.

---

## 2. HTTP → gRPC 성능 개선을 이 환경에서 측정할 수 있는가

### 2-1. 워크로드 구조 (측정 대상)

`EnrollmentService.enroll()` 코드 기준 (⚠️ k6 헤더 주석의 `SELECT ... FOR UPDATE` 설명은 **낡음**):

```
enroll(memberId, lectureId):
  ① validateActiveMember(memberId)   → member-service HTTP 호출   ┐ 내부 호출 2회
  ② lectureRestClient.get(lectureId) → lecture-service HTTP 호출  ┘ (트랜잭션/락 밖)
  ③ 임계 구역(@Transactional):
       existsBy... (SELECT)
       decrementRemainingSeats (원자적 UPDATE ... WHERE remaining_seats > 0)  ← FOR UPDATE 아님, 짧음
       INSERT
```

- 내부 호출(member, lecture) **2회가 매 요청마다** 크리티컬 패스에 있고, **락으로 가려지지 않는다.**
- 임계 구역은 `SELECT FOR UPDATE`가 아니라 **원자적 UPDATE**라 직렬화되지 않고 짧다.

→ **결론: gRPC 전환은 이 워크로드에서 의미 있는 변수가 맞다.** 없앨 수 있는 실제 HTTP 오버헤드(직렬화 CPU, 커넥션 처리)가 크리티컬 패스에 존재한다.

### 2-2. 그런데 "지금 환경에서 깨끗이 측정"은 어렵다

| # | 이유 | 설명 |
|---|---|---|
| 1 | **병목이 내부 호출이 아닐 확률이 큼** | gRPC는 *서비스 간 호출 구간*만 빠르게 한다. 4 vCPU에 요청당 DB 3~4회면 **CPU나 MySQL이 먼저 천장**. 그러면 내부 호출을 아무리 빠르게 해도 max RPS는 안 움직인다 → gRPC가 실제로 더 효율적이어도 "차이 없음"으로 나온다. |
| 2 | **localhost 네트워크는 이미 거의 공짜** | 모든 서비스가 한 호스트의 도커 브리지 안. 네트워크 지연 ≈ 0. gRPC의 큰 이득(HTTP/2 멀티플렉싱·전송 효율)이 대부분 사라지고, 남는 건 **직렬화 CPU 절감**뿐인데 member/lecture 페이로드가 작아 그 델타도 작다. |
| 3 | **노이즈 >> 신호** | 기대 개선폭이 요청당 몇 ms / 몇 %인데, co-located k6 + 관측 스택이 훔치는 CPU 변동이 그보다 크다. 델타가 노이즈에 묻힌다. |

### 2-3. 측정 가능하게 만들려면 (순서)

**① 먼저 "진짜 병목"을 찾는다 — 이게 안 되면 나머지는 무의미**

- 각 컨테이너 CPU (`docker stats`): 포화 시 enrollment/member/lecture/mysql 중 누가 100%인가?
- 요청 시간에서 **내부 호출(member+lecture)이 차지하는 비중** — 작으면 gRPC로 얻을 게 없다.
- MySQL이 벽이면 → gRPC 무관, 결론 낼 수 없음.

**② max RPS만 보지 말고 "직접 효과"를 본다**

| 지표 | 의미 |
|---|---|
| 내부 홉 지연 (enrollment→lecture/member p50·p95) | **직접 효과** — 여기서 차이가 먼저 보인다 |
| enrollment-service 요청당 CPU | protobuf 직렬화의 실질 이득 |
| max RPS | 위 홉이 **병목일 때만** 따라 올라간다 (2차 효과) |

**③ 교란 요인 제거**

- k6 별도 머신(최선) / 안 되면 관측 스택 끄기
- Docker CPU 상향, JVM 워밍업 후 측정, 실행 간 시드 리셋 고정 (`DELETE FROM enrollments; remaining_seats = max_enrollment`)

**④ 신호 증폭 (선택)**

- DB에 여유를 줘서 DB가 병목이 안 되게 하거나, 내부 홉만 떼어 **마이크로벤치**(enrollment→lecture 호출을 루프)로 HTTP vs gRPC 순수 비교. 여기서 확실한 숫자를 얻고 E2E RPS는 보조 지표로 둔다.

### 2-4. 요약

| 질문 | 답 |
|---|---|
| gRPC 전환이 RPS에 영향 줄 여지가 있나? | **있음** — 내부 호출 2회가 락 밖 크리티컬 패스에 있음 |
| 지금 구성으로 그걸 깨끗이 측정 가능? | **어려움** — 병목이 CPU/MySQL일 확률 높고, localhost라 gRPC 이득 작고, 노이즈가 큼 |
| 그럼 뭘 해야? | 먼저 병목 확인 → 내부 홉 지연/CPU를 **직접** 측정 → 교란 제거 → 필요시 홉만 격리 벤치 |

> **한 줄 결론**: "RPS가 올랐나?"를 바로 묻기 전에 **"내부 HTTP 호출이 정말 병목인가?"** 를 먼저 확인해야 한다.
> 병목이 아니면 gRPC가 아무리 빨라도 RPS는 그대로다.

---

## 부록 A. 이번에 함께 정리한 환경 이슈

측정 준비 과정에서 발견해 고친 것들 (참고용):

- **Loki 데이터 미표시**: Alloy `service` 라벨이 컨테이너 풀네임 → compose 서비스 라벨로 교체. Loki config 마운트 경로 불일치 수정. Grafana 데이터소스 프로비저닝 파일 추가.
- **Alloy `\x00` 경고 반복**: `loki.source.docker`(데몬 스트리밍 API, 로테이션 경계에서 null 바이트) → `loki.source.file`(파일 직접 tail)로 전환. 백로그 재전송 429 대응으로 Loki 수집 한도 상향.
- **consul DEBUG 스팸**: `-dev` 기본 DEBUG → `-log-level=info`. (디스커버리 클라이언트의 2초 주기 블로킹 쿼리 content-type 경고)
- **DB 스키마 드리프트**: 라이브 `lecture_seats`에 `remaining_seats` 컬럼 누락 → `schema.sql`에 맞춰 컬럼·CHECK 제약 추가하고 `max_enrollment - 기존수강수`로 백필.
- **stale jar**: 실행 중 enrollment-service가 옛 COUNT 기반 코드였음 → 현재 소스(원자적 `remaining_seats` 차감)로 재빌드·교체. (`Dockerfile`에 `chmod +x gradlew` 추가 — gradlew이 100644로 커밋돼 있어 빌드 실패하던 것 수정)
