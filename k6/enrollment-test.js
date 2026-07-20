import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

/**
 * 수강 신청 스파이크(strike) 테스트 — 정원 제한 강의의 동시성 정합성 검증.
 *
 * 시나리오: 평상시(20 req/s) 트래픽이 흐르다가 순간적으로 약 3,000명 규모로
 *          트래픽이 "튀는(spike)" 상황을 재현한다. 대상 강의(lecture 1)는
 *          정원(max_enrollment)이 100석뿐인 "선착순 인기 강의"다.
 *
 * ── 검증 포인트 ──────────────────────────────────────────────────────
 *  1. 정합성(오버셀 0): 3,000명이 몰려도 저장되는 수강 신청은 정확히 100건.
 *     비관적 락(SELECT ... FOR UPDATE)이 임계 구역을 직렬화하므로
 *     정원(count)과 저장(insert) 사이 레이스가 없어야 한다.
 *       → 실행 후:  SELECT COUNT(*) FROM enrollments WHERE lecture_id=1;  == 100 이어야 함.
 *         (101 이상이면 오버셀 = 정합성 깨짐 = 락 실패)
 *  2. 방어 응답: 정원이 찬 뒤의 신청은 5xx가 아니라 409(정원 초과)로 즉시 거절.
 *  3. 스파이크 내구성: 급증 구간에서 5xx/타임아웃 없이 버티는지, 응답 지연이
 *     어떻게 늘었다 회복되는지 관찰.
 *
 * ── UI로 보기 ────────────────────────────────────────────────────────
 *  라이브 대시보드:  실행 중 http://127.0.0.1:5665
 *    K6_WEB_DASHBOARD=true k6 run k6/enrollment-test.js
 *  HTML 리포트:
 *    k6 run -o 'web-dashboard=export=k6/report.html' k6/enrollment-test.js
 *
 * ── 게이트웨이 경유 ──────────────────────────────────────────────────
 *  요청은 단일 진입점인 게이트웨이(:7070)를 거친다. 게이트웨이는 위조 방지로
 *  클라이언트가 보낸 X-Member-Id 를 버리고, "로그인 세션"에 저장된 memberId 만
 *  X-Member-Id 로 실어 준다(MemberIdRelayFilter). 따라서 헤더를 직접 넣는 대신
 *  먼저 로그인해 세션 쿠키(SESSION)를 얻어야 한다.
 *
 *  ★ 세션은 VU 당 1회만 만든다.
 *    게이트웨이 세션 저장소(InMemoryWebSessionStore)는 기본 상한이 10,000개다.
 *    iteration 마다 로그인하면 수만 개의 세션이 쌓여 상한을 넘고, 이후 로그인이
 *    전부 500(Max sessions limit reached)으로 떨어진다. 그래서 각 VU 는 첫
 *    iteration 에서 한 번만 로그인하고, 이후엔 그 세션 쿠키를 재사용한다.
 *    → 동시 세션 수 ≤ 활성 VU 수(maxVUs) 라 상한에 걸리지 않는다.
 *
 *  처리 경로:
 *    ① (VU 첫 회만) POST /auth/login → gateway → member-service(자격증명 검증)
 *                                    → 세션 생성(SESSION 쿠키, VU 쿠키자에 보관)
 *    ② (매 iteration) POST /enrollments → gateway(세션의 memberId 를 X-Member-Id 로 주입)
 *                        → enrollment-service → member-service(활성회원 검증)
 *                        → lecture-service(강의 조회)
 *                        → DB(잠금 행 FOR UPDATE → 중복확인 → 정원검증 → INSERT)
 *
 *  ※ 회원은 VU 당 고정(VU id 로 매핑)이라, 같은 VU 가 좌석을 이미 확보한 뒤의
 *    재신청은 "중복 409"가 된다. 정원 초과 409 와 섞이지만 오버셀 검증(정확히
 *    100건 INSERT)에는 영향이 없다.
 *
 *  ※ loadtest 회원(loadtest_1000~)은 db/data.sql 에서 로그인 가능한 실제 BCrypt
 *    해시(평문 'test1234')로 시드되어 있어야 한다. placeholder 해시면 로그인 실패.
 *
 *  ※ 매 재실행 전 이전 신청 기록을 지워야 좌석 100개가 다시 비고 검증이 반복된다:
 *    docker exec lxp-msa-mysql-1 mysql -usohee -psohee LXP -e "DELETE FROM enrollments;"
 * ────────────────────────────────────────────────────────────────────
 */

const BASE = __ENV.BASE_URL || 'http://localhost:7070';        // 게이트웨이(단일 진입점) 경유
const BASE_MEMBER_ID = Number(__ENV.BASE_MEMBER_ID || 1000);   // loadtest 회원 시작 id
const MEMBER_POOL = Number(__ENV.MEMBER_POOL || 12000);        // 시드한 회원 수(id 1000~12999)
const HOT_LECTURE_ID = Number(__ENV.LECTURE_ID || 1);          // 정원 100석 선착순 인기 강의
const PASSWORD = __ENV.PASSWORD || 'test1234';                 // loadtest 회원 공통 비밀번호

// 200(로그인 성공)/201(좌석 확보)/409(정원 초과) 만 "정상 처리"로 본다. 이걸 지정하지
// 않으면 k6 기본값이 400 이상을 전부 실패(http_req_failed)로 집계해 strike 의 대량 409
// 때문에 지표가 무의미해진다.
http.setResponseCallback(http.expectedStatuses(200, 201, 409));

// 커스텀 지표: 좌석 확보 성공(201)과 정원 초과 거절(409) 비율을 분리해서 본다.
const createdRate = new Rate('enroll_created_201');          // 실제 좌석 확보(INSERT) 성공
const rejectedFullRate = new Rate('enroll_rejected_full_409'); // 정원 초과로 정상 거절
const loginOkRate = new Rate('login_ok_200');                // 세션 확보(로그인) 성공

// VU 별 상태(각 VU 는 독립된 JS 런타임 → 모듈 스코프 변수는 VU 마다 따로 유지된다).
// 첫 iteration 에서 로그인에 성공하면 true 로 바꿔, 이후 iteration 은 로그인을 건너뛴다.
let sessionReady = false;

export const options = {
  // ramping-arrival-rate: 초당 요청 수(RPS)를 직접 제어하는 개방형 모델.
  // 서버가 느려져도 목표 RPS를 유지하므로 "부하"를 정확히 인가한다.
  // 총 약 3분. 워밍업 뒤 250→500→1000→2000→3000 req/s 로 가파르게 끌어올려
  // 시스템이 꺾이는 포화 지점(knee)을 찾는다. 각 단계를 유지해 그 부하에서의
  // 안정 처리량/지연을 읽는다.
  // → 대시보드에서 어느 RPS부터 처리량이 목표를 못 따라가고(포화) 지연이
  //   급격히 치솟으며 VU가 폭증하는지 계단 곡선으로 드러난다.
  noCookiesReset: true,

  scenarios: {
    load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,          // 워밍업 시작 RPS
      timeUnit: '1s',
      preAllocatedVUs: 500,   // 미리 확보해 둘 VU
      maxVUs: 4000,           // 포화 시 락 대기로 VU가 폭증하므로 넉넉히 확보
      stages: [
        { target: 50,   duration: '10s' }, // 워밍업(커넥션/JIT 예열)
        { target: 250,  duration: '10s' }, // 1단계로 상승
        { target: 250,  duration: '25s' }, //   유지 — 안정 처리량/지연 측정
        { target: 500,  duration: '10s' }, // 2단계로 상승
        { target: 500,  duration: '25s' }, //   유지
        { target: 1000, duration: '10s' }, // 3단계로 상승
        { target: 1000, duration: '25s' }, //   유지
        { target: 2000, duration: '10s' }, // 4단계로 상승(포화 진입 예상)
        { target: 2000, duration: '25s' }, //   유지
        { target: 3000, duration: '10s' }, // 5단계로 상승(과부하)
        { target: 3000, duration: '20s' }, //   유지: 처리량 한계/지연 폭증 관찰
        { target: 0,    duration: '10s' }, // 램프다운(회복 관찰)
      ],
    },
  },
  thresholds: {
    // 5xx/타임아웃/400 등 "진짜 실패"만 집계(200·201·409는 위 콜백으로 정상 처리).
    'http_req_failed': ['rate<0.01'],
    // 스파이크 구간엔 락 큐잉으로 지연이 치솟는다. 넘기면 그 자체가 관찰 포인트.
    // 로그인 지연이 섞이지 않게 수강 신청 요청만 골라 본다.
    'http_req_duration{name:POST /enrollments}': ['p(95)<3000'],
    'checks': ['rate>0.99'],
  },
};

export default function () {
  // 회원을 VU 단위로 유니크하게 매핑(VU 당 한 명 고정) → 세션도 VU 당 하나.
  // idInTest 는 테스트 전체에서 VU 마다 유일하다. maxVUs ≤ MEMBER_POOL 이면 충돌 없음.
  const memberId = BASE_MEMBER_ID + (exec.vu.idInTest % MEMBER_POOL);
  const loginId = `loadtest_${memberId}`;

  // ① 로그인: VU 당 첫 iteration 에서만 수행(세션 저장소 상한 회피). 성공 시 SESSION
  //    쿠키가 VU 쿠키자에 저장돼 이후 iteration 요청에 자동으로 실려 나간다.
  if (!sessionReady) {
    const loginRes = http.post(
      `${BASE}/auth/login`,
      JSON.stringify({ loginId, password: PASSWORD }),
      {
        headers: { 'Content-Type': 'application/json' },
        tags: { name: 'POST /auth/login' },
      }
    );

    const loggedIn = loginRes.status === 200;
    loginOkRate.add(loggedIn);
    check(loginRes, { 'login ok (200)': (r) => r.status === 200 });

    // 실패 시 세션이 없어 게이트웨이가 X-Member-Id 를 못 실어 준다 → 이번 iteration 은
    // 신청을 건너뛰고 다음 iteration 에서 다시 로그인 시도.
    if (!loggedIn) {
      return;
    }
    sessionReady = true;
  }

  // ② 수강 신청: X-Member-Id 는 넣지 않는다(게이트웨이가 세션의 memberId 로 주입).
  //    SESSION 쿠키는 VU 쿠키자에 저장돼 자동으로 함께 전송된다.
  const res = http.post(
    `${BASE}/enrollments`,
    JSON.stringify({ lectureId: HOT_LECTURE_ID }),
    {
      headers: { 'Content-Type': 'application/json' },
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
