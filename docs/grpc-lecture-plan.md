# gRPC 적용 계획 — 수강 신청(enrollment) → 강의(lecture) 내부 호출

## 0. 범위와 결정 사항

수강 신청 흐름에서 **enrollment → lecture 호출 딱 하나만** gRPC로 바꾼다.
member 호출(활성 회원 검증)은 REST 그대로 둔다.

| 항목 | 결정 | 근거 |
|---|---|---|
| 대상 호출 | `EnrollmentService.enroll()` 안의 lecture 조회 1건 | "수강 신청(lecture)에만" — 최소 수직 슬라이스 |
| 연동 라이브러리 | Spring gRPC 공식 스타터 (`org.springframework.grpc`) | 이미 Boot 4.1 + Spring Cloud 스택. 04장 교재가 Boot 4.1에서 검증·버전핀 완료 |
| 서버 / 클라이언트 | lecture = gRPC 서버, enrollment = gRPC 클라이언트 | enrollment가 lecture를 부른다 |
| 채널 주소 | `static://lecture-service:9091` (이름 기반) | lecture는 1대. 이름이라 컨테이너 IP 변경에도 견딤. Consul은 gRPC 포트 미등록이라 부적합 |
| member 호출 | 변경 없음 (REST 유지) | 범위 밖 |

### 왜 Consul이 아니라 static인가 (짧은 요약)

- 여러 대로 뜨는 건 **클라이언트(enrollment, replicas=3)** 지 서버가 아니다. 로드밸런싱 대상인
  lecture는 1대라 서버 디스커버리가 불필요.
- Consul은 서비스의 **HTTP 포트만** 등록한다. gRPC(9091)는 같은 프로세스의 다른 리스너라
  Consul이 모른다. 게다가 lecture는 지금 Consul에 등록조차 안 돼 있다.
- 채널을 IP가 아니라 **서비스 이름**(`lecture-service`)으로 잡으면 Docker DNS가 매번 현재 IP로
  풀어주므로, 재시작·주소 변경에 견딘다. 걱정하던 문제는 이름 기반이면 발생하지 않는다.
- 업그레이드 경로: lecture를 여러 대로 스케일하게 되면 그때 `dns:///lecture-service:9091` +
  `round_robin`으로 바꾼다(교재 04장 4절 방식). Consul-for-gRPC는 마지막 선택지.

---

## 1. 계약: lecture.proto

enrollment가 lecture에서 실제로 쓰는 데이터는 `{lectureId, maxEnrollment}` 두 개뿐이다
(현재 `LectureResponse`, enrollment 쪽 `Lecture` record와 동일). 그래서 계약이 아주 짧다.

`[신규]` `lecture-service/src/main/proto/lecture.proto`
`[신규]` `enrollment-service/src/main/proto/lecture.proto` (↑와 **바이트까지 동일하게 복제**)

```proto
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.ohgiraffers.lecture.grpc";   // 양쪽 서비스가 공유하는 중립 패키지명

package lecture;

// 수강 신청 검증용 강의 조회. unary(요청 1 → 응답 1).
service LectureInternalService {
  rpc GetLecture(GetLectureRequest) returns (GetLectureReply) {}
}

message GetLectureRequest {
  int64 lecture_id = 1;
}

message GetLectureReply {
  int64 lecture_id = 1;
  int32 max_enrollment = 2;
}
```

- 폴리레포라 `.proto`를 두 서비스에 복제한다. 서버·클라이언트가 같은 계약에서 코드를 생성해야
  타입이 맞물린다. (실무는 공유 저장소/BSR로 복제를 없애지만 지금은 학습용으로 복제)
- 에러(존재하지 않음 / 비공개)는 응답 필드가 아니라 **gRPC 상태 코드**로 전달한다(아래 3, 4절).

---

## 2. 빌드 설정 (코드 생성) — 두 서비스 공통

두 `build.gradle`에 protobuf 플러그인 + Spring gRPC 의존성 + 코드 생성 블록을 추가한다.
버전은 교재 04장이 Boot 4.1에서 검증한 값을 그대로 쓴다.

공통 추가:
- 플러그인: `id 'com.google.protobuf' version '0.9.4'`
- BOM: `org.springframework.grpc:spring-grpc-dependencies:1.0.3` 를 `dependencyManagement.imports`에 추가
- 버전 핀(ext): `springGrpcVersion=1.0.3`, `grpcVersion=1.77.1`, `protobufVersion=4.33.4`
- 공통 의존성:
  - `io.grpc:grpc-stub:${grpcVersion}`
  - `io.grpc:grpc-protobuf:${grpcVersion}`
  - `compileOnly 'org.apache.tomcat:annotations-api:6.0.53'`
- `protobuf { protoc {...}; plugins { grpc {...} }; generateProtoTasks { all()*.plugins { grpc {} } } }`

서비스별 차이 (딱 이 한 줄):
- **lecture (서버)**: `implementation "org.springframework.grpc:spring-grpc-server-spring-boot-starter:${springGrpcVersion}"`
- **enrollment (클라이언트)**: `implementation "org.springframework.grpc:spring-grpc-client-spring-boot-starter:${springGrpcVersion}"`

### 자주 깨지는 함정 3가지 (교재 04장 2-2와 동일)
1. 스타터만으론 컴파일 안 됨 → `grpc-stub`, `grpc-protobuf`를 **직접** 추가해야 함
   (증상: `package io.grpc.stub does not exist`)
2. 코드 생성기 버전 ≠ 런타임 버전이면 깨짐 → protoc/grpc 버전을 런타임과 **똑같이** 핀
   (증상: `cannot find symbol: method blockingV2UnaryCall(...)`)
3. `@javax.annotation.Generated`가 JDK 9+에서 빠짐 → `annotations-api`를 compileOnly로 추가
   (증상: `package javax.annotation does not exist`)

### 검증
각 서비스에서 코드 생성만 먼저 돌려 스텁이 생기는지 확인:
```bash
./gradlew generateProto
# build/generated/source/proto/main/grpc/.../LectureInternalServiceGrpc.java 가 보이면 성공
```

---

## 3. 서버: lecture-service

`[신규]` `lecture-service/.../lecture/grpc/GrpcLectureService.java`

- `LectureInternalServiceGrpc.LectureInternalServiceImplBase`를 상속, `getLecture`만 override.
- 로직은 기존 내부 REST 컨트롤러(`LectureController.findLectureById`)와 동일:
  1. `lectureRepository.findById(id)` → 없으면 `Status.NOT_FOUND` 로 `onError`
  2. `status != PUBLIC` → `Status.FAILED_PRECONDITION` 로 `onError`
  3. 정상 → `GetLectureReply{lecture_id, max_enrollment}` 를 `onNext` 후 `onCompleted`
- 클래스에 `@org.springframework.grpc.server.service.GrpcService` 부착 (= gRPC의 @RestController).

에러 매핑 규약:
| 상황 | gRPC Status | 메시지 |
|---|---|---|
| 강의 없음 | `NOT_FOUND` | LECTURE_NOT_FOUND |
| 비공개 강의 | `FAILED_PRECONDITION` | LECTURE_NOT_ACCESSIBLE |

`[수정]` `lecture-service/src/main/resources/application.yml`
```yaml
spring:
  grpc:
    server:
      port: 9091    # REST(9081, HTTP/1.1)와 별개 포트로 gRPC(HTTP/2) 리스너 추가
```

> 기존 REST 내부 API(`/api/v1/lectures/{id}`)는 **당장 지우지 않는다**. gRPC 검증이 끝나고
> 다른 소비자가 없을 때 별도 커밋으로 제거. (지금은 롤백 안전판)

### 검증
```bash
# 로그에 gRPC 서버가 9091에서 떴는지 확인
# "gRPC Server started, listening on address: ...:9091"
```

---

## 4. 클라이언트: enrollment-service

`[신규]` `enrollment-service/.../enrollment/grpc/GrpcLectureClient.java`

- 생성자에서 `GrpcChannelFactory.createChannel("lecture-service")`로 채널을 만들고
  `LectureInternalServiceGrpc.newBlockingStub(channel)` 보관 (채널 1개 재사용).
- `getLecture(long id)` 메서드:
  - `stub.getLecture(GetLectureRequest.newBuilder().setLectureId(id).build())` 호출
  - `StatusRuntimeException` 잡아서 enrollment의 `BusinessException`으로 변환:

| gRPC Status | enrollment ErrorCode |
|---|---|
| `NOT_FOUND` | `LECTURE_NOT_FOUND` |
| `FAILED_PRECONDITION` | `ENROLLMENT_LECTURE_NOT_ACCESSIBLE` |
| 그 외 | `COMMON_INTERNAL_ERROR` |

`[수정]` `EnrollmentService.enroll()` — REST 호출부를 gRPC로 교체:
```java
// 변경 전: lectureRestClient.get().uri("api/v1/lectures/{id}", lectureId)...
// 변경 후:
GetLectureReply reply = grpcLectureClient.getLecture(lectureId);
Lecture lecture = new Lecture(reply.getLectureId(), reply.getMaxEnrollment());
```
- `lectureRestClient` 필드/주입 제거. `memberRestClient`는 그대로 둔다.
- 이후 로직(정원 잠금 행, `enrollmentProcessor.enroll`)은 손대지 않는다.

`[수정]` `enrollment-service/src/main/resources/application.yml`
```yaml
spring:
  grpc:
    client:
      channels:                        # 복수형(channels), 하위 주소 키는 address
        lecture-service:
          address: ${LECTURE_GRPC_TARGET:static://localhost:9091}
          # 로컬 단독 실행: localhost:9091
          # 도커: compose에서 LECTURE_GRPC_TARGET=static://lecture-service:9091 주입
```

> 함정(검증 중 실제로 겪음): Spring gRPC 1.0.3 클라이언트 키는
> `spring.grpc.client.channels.<name>.address`. `channel`(단수)/`target`으로 쓰면 조용히
> 무시되고 기본 채널(`static://localhost:9090`)로 폴백해 `UNAVAILABLE: Connection refused`가 난다.

---

## 5. 도커 / 실행 환경

`[수정]` `docker-compose.yml`
- `enrollment-service` environment에 `LECTURE_GRPC_TARGET: static://lecture-service:9091` 추가.
- lecture의 gRPC 포트(9091)는 **네트워크 내부 전용**이라 host로 publish 불필요
  (enrollment이 같은 `lxp-net`에서 이름으로 접근). 필요 시 디버깅용으로만 `9091:9091` 노출.
- gateway는 변경 없음 (내부 호출이라 gateway 경유 안 함).

---

## 6. 검증 시나리오

| # | 케이스 | 기대 결과 |
|---|---|---|
| 1 | 정상 강의로 수강 신청 | 201 Created (gRPC로 강의 조회됨) |
| 2 | 존재하지 않는 lectureId | LECTURE_NOT_FOUND (404) |
| 3 | 비공개(PUBLIC 아님) 강의 | ENROLLMENT_LECTURE_NOT_ACCESSIBLE (404) |
| 4 | 정원 초과 강의 | ENROLLMENT_CAPACITY_EXCEEDED (409) — 기존 로직 유지 확인 |
| 5 | 중복 신청 | ENROLLMENT_ALREADY_EXISTS (409) — 기존 로직 유지 확인 |

- 로컬: lecture(bootRun) + enrollment(bootRun) 띄우고 `enrollment-service/request.http`로 호출.
- gRPC 홉 확인: lecture 로그에 `GetLecture` 호출 debug 로그를 남겨 실제로 gRPC를 탔는지 본다.
- 기존 REST 경로와 동작이 동일한지(같은 에러코드·상태) 대조.

---

## 7. 작업 순서 (체크리스트)

- [ ] 1. `lecture.proto` 작성 → lecture, enrollment 양쪽에 복제
- [ ] 2. lecture `build.gradle` 설정 + `generateProto`로 스텁 생성 확인
- [ ] 3. enrollment `build.gradle` 설정 + `generateProto`로 스텁 생성 확인
- [ ] 4. lecture `GrpcLectureService` 구현 + `application.yml` gRPC 포트 → 서버 기동 확인
- [ ] 5. enrollment `GrpcLectureClient` 구현 + `EnrollmentService` 호출부 교체 + `application.yml` 채널
- [ ] 6. `docker-compose.yml` 환경변수 추가
- [ ] 7. 검증 시나리오 1~5 실행
- [ ] 8. (후속, 별도 커밋) lecture의 미사용 REST 내부 API 제거 검토

---

## 참고: 손대지 않는 것

- member 호출(REST) — 범위 밖
- enrollment 정원/동시성 로직(`EnrollmentProcessor`, `LectureSeat`) — 강의 조회 방식만 바뀔 뿐
- gateway 라우팅, Consul 설정 — 내부 gRPC 호출과 무관
- lecture가 member를 부르는 REST 호출 — 범위 밖
