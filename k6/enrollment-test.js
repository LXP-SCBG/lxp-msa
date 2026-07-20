import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

/**
 * 수강 신청 부하(load) 테스트 — 처리량 증가에 따른 응답 특성 관찰.
 *
 * 시나리오: 초당 요청 수(RPS)를 계단식으로 서서히 끌어올리며 각 단계를
 *          일정 시간 유지한다. 부하가 오를수록 처리량(throughput)과 응답 지연
 *          (p95/p99)이 어떻게 변하는지, 시스템이 어느 지점에서 꺾이는지를
 *          대시보드에서 한눈에 볼 수 있게 하는 것이 목적이다.
 *          대상 강의(lecture 1)는 정원 제한이 있는 "선착순 인기 강의"라
 *          만석 이후 요청은 409로 방어되지만, 그 경로도 전체 스택
 *          (member/lecture 검증 → DB 락)을 그대로 태우므로 부하로서 유효하다.
 *
 * ── 관찰 포인트 ──────────────────────────────────────────────────────
 *  1. 처리량 확장성: RPS 계단이 오를 때 실제 처리량이 선형으로 따라오는지,
 *     아니면 특정 단계에서 포화(saturation)되는지.
 *  2. 응답 지연 추이: 각 단계별 http_req_duration p95/p99 가 어떻게 증가하는지.
 *  3. 안정성: 부하가 올라도 5xx/타임아웃 없이 201(좌석 확보)·409(정원 초과)로
 *     정상 처리되는지.
 *
 * ── UI로 보기 ────────────────────────────────────────────────────────
 *  라이브 대시보드:  실행 중 http://127.0.0.1:5665
 *    K6_WEB_DASHBOARD=true k6 run k6/enrollment-test.js
 *  HTML 리포트:
 *    k6 run -o 'web-dashboard=export=k6/report.html' k6/enrollment-test.js
 *
 * ── 주의 ────────────────────────────────────────────────────────────
 *  게이트웨이(:7070)는 위조 방지로 X-Member-Id 를 제거하므로 로그인 없이
 *  부하를 주려면 enrollment-service 를 직접(:9082) 호출한다.
 *
 *  enroll 처리 경로(요청 1건당):
 *    enrollment-service → member-service(활성회원 검증) → lecture-service(강의 조회)
 *                       → DB(잠금 행 FOR UPDATE → 중복확인 → 정원검증 → INSERT)
 *
 *  ※ 매 재실행 전 이전 신청 기록을 지워야 좌석 100개가 다시 비고 검증이 반복된다:
 *    docker exec lxp-msa-mysql-1 mysql -usohee -psohee LXP -e "DELETE FROM enrollments;"
 * ────────────────────────────────────────────────────────────────────
 */

const BASE = __ENV.BASE_URL || 'http://localhost:9082';        // enrollment-service 직접 호출
const BASE_MEMBER_ID = Number(__ENV.BASE_MEMBER_ID || 1000);   // loadtest 회원 시작 id
const MEMBER_POOL = Number(__ENV.MEMBER_POOL || 12000);        // 시드한 회원 수(id 1000~12999)
const HOT_LECTURE_ID = Number(__ENV.LECTURE_ID || 1);          // 정원 100석 선착순 인기 강의

// 201/409 만 "정상 처리"로 본다. 이걸 지정하지 않으면 k6 기본값이 400 이상을 전부
// 실패(http_req_failed)로 집계해 strike 의 대량 409 때문에 지표가 무의미해진다.
http.setResponseCallback(http.expectedStatuses(201, 409));

// 커스텀 지표: 좌석 확보 성공(201)과 정원 초과 거절(409) 비율을 분리해서 본다.
const createdRate = new Rate('enroll_created_201');          // 실제 좌석 확보(INSERT) 성공
const rejectedFullRate = new Rate('enroll_rejected_full_409'); // 정원 초과로 정상 거절

export const options = {
  // ramping-arrival-rate: 초당 요청 수(RPS)를 직접 제어하는 개방형 모델.
  // 서버가 느려져도 목표 RPS를 유지하므로 "부하"를 정확히 인가한다.
  // 총 약 3분. 워밍업 뒤 250→500→1000→2000→3000 req/s 로 가파르게 끌어올려
  // 시스템이 꺾이는 포화 지점(knee)을 찾는다. 각 단계를 유지해 그 부하에서의
  // 안정 처리량/지연을 읽는다.
  // → 대시보드에서 어느 RPS부터 처리량이 목표를 못 따라가고(포화) 지연이
  //   급격히 치솟으며 VU가 폭증하는지 계단 곡선으로 드러난다.
  scenarios: {
    load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,          // 워밍업 시작 RPS
      timeUnit: '1s',
      preAllocatedVUs: 500,   // 미리 확보해 둘 VU
      maxVUs: 6000,           // 포화 시 락 대기로 VU가 폭증하므로 넉넉히 확보
      stages: [
        { target: 50,   duration: '10s' }, // 워밍업(커넥션/JIT 예열)
        { target: 250,  duration: '10s' }, // 1단계로 상승
        { target: 250,  duration: '25s' }, //   유지 — 안정 처리량/지연 측정
        { target: 500,  duration: '10s' }, // 2단계로 상승
        { target: 500,  duration: '25s' }, //   유지
        { target: 1000, duration: '10s' }, // 3단계로 상승
        { target: 1000, duration: '25s' }, //   유지
        { target: 2000, duration: '10s' }, // 4단계로 상승(포화 진입 예상)
        { target: 2000, duration: '25s' }, //   유지
        { target: 3000, duration: '10s' }, // 5단계로 상승(과부하)
        { target: 3000, duration: '20s' }, //   유지: 처리량 한계/지연 폭증 관찰
        { target: 0,    duration: '10s' }, // 램프다운(회복 관찰)
      ],
    },
  },
  thresholds: {
    // 5xx/타임아웃/400 등 "진짜 실패"만 집계(201·409는 위 콜백으로 정상 처리).
    // 과부하 구간에서 이 임계를 넘기면 그게 바로 포화/한계 신호(관찰 포인트).
    'http_req_failed': ['rate<0.01'],
    // 포화 지점 이후 지연이 이 선을 넘어 치솟는 지점이 knee 다.
    'http_req_duration': ['p(95)<2000'],
    'checks': ['rate>0.99'],
  },
};

export default function () {
  // 전역 반복 인덱스로 회원을 유니크하게 매핑 → 요청마다 다른 회원.
  // (같은 회원 재사용 시 "중복(409)"이 섞여 정원 초과 409 집계가 오염되므로 방지)
  const memberId = BASE_MEMBER_ID + (exec.scenario.iterationInTest % MEMBER_POOL);

  const res = http.post(
    `${BASE}/enrollments`,
    JSON.stringify({ lectureId: HOT_LECTURE_ID }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-Member-Id': String(memberId),
      },
      tags: { name: 'POST /enrollments' },
    }
  );

  createdRate.add(res.status === 201);
  rejectedFullRate.add(res.status === 409);

  // 201=좌석 확보 성공, 409=정원 초과(정상 방어) → 둘 다 서버가 "정상 처리"한 것.
  // 5xx / 타임아웃 / 400(헤더 누락 등)은 진짜 실패로 본다.
  check(res, {
    'handled (201 or 409)': (r) => r.status === 201 || r.status === 409,
    'no server error (<500)': (r) => r.status < 500,
  });
}
