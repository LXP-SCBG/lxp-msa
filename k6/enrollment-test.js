import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { Rate } from 'k6/metrics';

/**
 * 수강 신청 스파이크(strike) 테스트 — 정원 제한 강의의 동시성 정합성 검증.
 *
 * 시나리오: 평상시(20 req/s) 트래픽이 흐르다가 순간적으로 약 3,000명 규모로
 *          트래픽이 "튀는(spike)" 상황을 재현한다. 대상 강의(lecture 1)는
 *          정원(max_enrollment)이 1000석인 "선착순 인기 강의"다.
 *
 * ── 검증 포인트 ──────────────────────────────────────────────────────
 *  1. 정합성(오버셀 0): 3,000명이 몰려도 저장되는 수강 신청은 정확히 1000건.
 *     비관적 락(SELECT ... FOR UPDATE)이 임계 구역을 직렬화하므로
 *     정원(count)과 저장(insert) 사이 레이스가 없어야 한다.
 *       → 실행 후:  SELECT COUNT(*) FROM enrollments WHERE lecture_id=1;  == 1000 이어야 함.
 *         (1001 이상이면 오버셀 = 정합성 깨짐 = 락 실패)
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
 * ── 게이트웨이 경유 + "로그인은 측정 밖에서" ─────────────────────────
 *  요청은 단일 진입점인 게이트웨이(:7070)를 거친다. 게이트웨이는 위조 방지로
 *  클라이언트가 보낸 X-Member-Id 를 버리고, "로그인 세션"에 저장된 memberId 만
 *  X-Member-Id 로 실어 준다(MemberIdRelayFilter). 따라서 헤더를 직접 넣는 대신
 *  먼저 로그인해 세션 쿠키(SESSION)를 얻어야 한다.
 *
 *  ★ 로그인은 부하 측정 대상이 아니다 → setup() 에서 미리 끝낸다.
 *    로그인을 측정 구간(default 함수) 안에서 하면, ramping-arrival-rate 가
 *    램프업마다 새 VU 를 투입할 때 각 VU 의 첫 iteration 이 느린 로그인 호출에
 *    묶여 enroll 요청을 못 쏜다. 그 결과 (a) 목표 RPS 를 못 따라가 실제
 *    enroll 처리량이 낮게 찍히고, (b) http_reqs 에 로그인이 섞여 지표가 오염된다.
 *    → 그래서 setup() 에서 필요한 세션을 전부 미리 만들어 SESSION 쿠키 값만
 *      뽑아 두고(이 시간은 시나리오 지표에 집계되지 않음), 측정 구간에서는
 *      VU 마다 그 쿠키를 쿠키자에 1회 주입한 뒤 순수 POST /enrollments 만 날린다.
 *      이렇게 하면 no-load 테스트(enroll 요청 1건/iteration)와 요청 모양이
 *      같아져(게이트웨이 hop 만 추가) 로드밸런싱 유/무 비교가 공정해진다.
 *
 *    ※ setup 의 로그인은 http.batch 로 병렬 처리한다(SETUP_BATCH 개씩). 순차로
 *      MAX_VUS(예: 4000)번 로그인하면 60초 기본 제한(setupTimeout)을 넘겨
 *      "setup() execution timed out" 으로 죽는다. 병렬화로 수 초 내에 끝낸다.
 *      안전장치로 setupTimeout 도 넉넉히 잡아 둔다.
 *
 *  ★ 세션 수 = maxVUs.
 *    VU→회원 매핑이 vu.idInTest 기반이라 활성 VU 마다 고유 세션이 필요하다.
 *    게이트웨이 세션 저장소(InMemoryWebSessionStore)는 기본 상한이 10,000개이므로
 *    maxVUs(≤10,000) 개면 상한에 걸리지 않는다.
 *
 *    ※ maxVUs 를 정원(1000)에 맞춰 낮추면 안 된다. 락 대기로 응답이 느려지면
 *      리틀의 법칙상 목표 RPS(3000)를 유지하는 데 필요한 동시 VU 가 폭증한다.
 *      VU 가 모자라면 k6 가 3000 RPS 를 못 쏴서 dropped_iterations 가 발생하고,
 *      그러면 scale 유/무 비교가 "서버 성능"이 아니라 "부하 생성기 한계" 비교가
 *      되어 오염된다. 그래서 maxVUs 는 넉넉히(기본 4000) 유지한다.
 *
 *  처리 경로:
 *    ① (setup, 측정 밖) VU 수만큼 POST /auth/login → gateway → member-service
 *                       (자격증명 검증) → 세션 생성 → SESSION 쿠키 값 수집
 *    ② (default 첫 iteration) 해당 VU 의 SESSION 쿠키를 쿠키자에 주입(네트워크 아님)
 *    ③ (매 iteration) POST /enrollments → gateway(세션의 memberId 를 X-Member-Id 로 주입)
 *                        → enrollment-service → member-service(활성회원 검증)
 *                        → lecture-service(강의 조회)
 *                        → DB(잠금 행 FOR UPDATE → 중복확인 → 정원검증 → INSERT)
 *
 *  ※ 회원은 VU 당 고정(VU id 로 매핑)이라, 같은 VU 가 좌석을 이미 확보한 뒤의
 *    재신청은 "중복 409"가 된다. 정원 초과 409 와 섞이지만 오버셀 검증(정확히
 *    1000건 INSERT)에는 영향이 없다.
 *
 *  ※ loadtest 회원(loadtest_1000~)은 db/data.sql 에서 로그인 가능한 실제 BCrypt
 *    해시(평문 'test1234')로 시드되어 있어야 한다. placeholder 해시면 로그인 실패.
 *
 *  ※ 매 재실행 전 이전 신청 기록을 지워야 좌석 1000개가 다시 비고 검증이 반복된다:
 *    docker exec lxp-msa-mysql-1 mysql -usohee -psohee LXP -e "DELETE FROM enrollments;"
 * ────────────────────────────────────────────────────────────────────
 */

const BASE = __ENV.BASE_URL || 'http://localhost:7070';        // 게이트웨이(단일 진입점) 경유
const BASE_MEMBER_ID = Number(__ENV.BASE_MEMBER_ID || 1000);   // loadtest 회원 시작 id
const MEMBER_POOL = Number(__ENV.MEMBER_POOL || 12000);        // 시드한 회원 수(id 1000~12999)
const HOT_LECTURE_ID = Number(__ENV.LECTURE_ID || 1);          // 정원 1000석 선착순 인기 강의
const PASSWORD = __ENV.PASSWORD || 'test1234';                 // loadtest 회원 공통 비밀번호
const MAX_VUS = Number(__ENV.MAX_VUS || 2000);                 // 활성 VU 상한 = 미리 만들 세션 수
const SETUP_BATCH = Number(__ENV.SETUP_BATCH || 100);          // setup 로그인 병렬 배치 크기

// 200(로그인 성공)/201(좌석 확보)/409(정원 초과) 만 "정상 처리"로 본다. 이걸 지정하지
// 않으면 k6 기본값이 400 이상을 전부 실패(http_req_failed)로 집계해 strike 의 대량 409
// 때문에 지표가 무의미해진다.
http.setResponseCallback(http.expectedStatuses(200, 201, 409));

// 커스텀 지표: 좌석 확보 성공(201)과 정원 초과 거절(409) 비율을 분리해서 본다.
const createdRate = new Rate('enroll_created_201');          // 실제 좌석 확보(INSERT) 성공
const rejectedFullRate = new Rate('enroll_rejected_full_409'); // 정원 초과로 정상 거절

// VU 별 상태(각 VU 는 독립된 JS 런타임 → 모듈 스코프 변수는 VU 마다 따로 유지된다).
// 첫 iteration 에서 setup 이 넘겨준 SESSION 쿠키를 쿠키자에 한 번 주입하고 true 로 바꾼다.
let cookieInjected = false;

export const options = {
  // ramping-arrival-rate: 초당 요청 수(RPS)를 직접 제어하는 개방형 모델.
  // 서버가 느려져도 목표 RPS를 유지하므로 "부하"를 정확히 인가한다.
  // 총 약 3분. 워밍업 뒤 250→500→1000→2000→3000 req/s 로 가파르게 끌어올려
  // 시스템이 꺾이는 포화 지점(knee)을 찾는다. 각 단계를 유지해 그 부하에서의
  // 안정 처리량/지연을 읽는다.
  // → 대시보드에서 어느 RPS부터 처리량이 목표를 못 따라가고(포화) 지연이
  //   급격히 치솟으며 VU가 폭증하는지 계단 곡선으로 드러난다.
  noCookiesReset: true, // iteration 사이 쿠키자를 유지 → 주입한 SESSION 쿠키가 계속 실려 나감

  // setup 에서 로그인 세션을 MAX_VUS 개 만든다. 병렬(batch)이라 빠르지만,
  // MAX_VUS 가 크거나 서버가 느릴 때를 대비해 기본 60초보다 넉넉히 잡는다.
  setupTimeout: '120s',

  scenarios: {
    load: {
      executor: 'ramping-arrival-rate',
      startRate: 50,          // 워밍업 시작 RPS
      timeUnit: '1s',
      preAllocatedVUs: 500,   // 미리 확보해 둘 VU
      maxVUs: MAX_VUS,        // 포화 시 락 대기로 VU가 폭증하므로 넉넉히 확보(= 세션 수)
      stages: [
        { target: 50,   duration: '10s' }, // 워밍업
        { target: 100,  duration: '10s' },
        { target: 100,  duration: '30s' }, //   유지 — 안정 처리량/지연 측정
        { target: 200,  duration: '10s' },
        { target: 200,  duration: '30s' }, //   유지
        { target: 400,  duration: '10s' },
        { target: 400,  duration: '30s' }, //   유지
        { target: 600,  duration: '10s' },
        { target: 600,  duration: '30s' }, //   유지: 포화 진입 예상 지점
        { target: 0,    duration: '10s' }, // 램프다운
      ],
    },
  },
  thresholds: {
    // 5xx/타임아웃/400 등 "진짜 실패"만 집계(200·201·409는 위 콜백으로 정상 처리).
    'http_req_failed': ['rate<0.01'],
    // 스파이크 구간엔 락 큐잉으로 지연이 치솟는다. 넘기면 그 자체가 관찰 포인트.
    // (default 함수엔 이제 enroll 요청만 있으므로 사실상 전체 = 수강 신청 지연이다.)
    'http_req_duration{name:POST /enrollments}': ['p(95)<3000'],
    'checks': ['rate>0.99'],
  },
};

// ── setup(): 측정 구간 밖에서 세션을 전부 미리 만든다 ──────────────────
// 반환값(cookies)은 모든 VU 에 공유된다. 여기서 발생하는 로그인 요청/지연은
// 시나리오 지표(http_reqs, http_req_duration 등)에 집계되지 않는다.
//
// 순차 로그인은 MAX_VUS 가 크면 setupTimeout(기본 60초)을 넘겨 죽으므로,
// http.batch 로 SETUP_BATCH 개씩 묶어 병렬 로그인한다.
export function setup() {
  const cookies = new Array(MAX_VUS);
  let ok = 0;

  for (let start = 0; start < MAX_VUS; start += SETUP_BATCH) {
    const end = Math.min(start + SETUP_BATCH, MAX_VUS);
    const reqs = [];
    for (let i = start; i < end; i++) {
      const memberId = BASE_MEMBER_ID + ((i + 1) % MEMBER_POOL);
      const jar = new http.CookieJar();   // ← 요청마다 빈 쿠키자
      reqs.push([
        'POST',
        `${BASE}/auth/login`,
        JSON.stringify({ loginId: `loadtest_${memberId}`, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' }, jar },   // ← jar 지정
      ]);
    }

    const responses = http.batch(reqs);
    for (let j = 0; j < responses.length; j++) {
      const res = responses[j];
      const jarCookie = res.cookies['SESSION'];
      const value = res.status === 200 && jarCookie && jarCookie[0] ? jarCookie[0].value : null;
      if (value) {
        ok++;
      } else if (start === SETUP_BATCH) {
        // 두 번째 배치의 첫 실패 샘플
        console.log(`[setup] 실패 샘플 status=${res.status} hasCookie=${!!jarCookie}`);
      }
      cookies[start + j] = value;
    }
    sleep(0.2);
  }

  console.log(`[setup] 세션 생성: ${ok}/${MAX_VUS} 성공`);
  if (ok === 0) {
    throw new Error('[setup] 로그인 세션을 하나도 만들지 못했습니다. loadtest 회원 시드/게이트웨이 상태를 확인하세요.');
  }
  return { cookies };
}

export default function (data) {
  // ② VU 별 첫 iteration 에서만 SESSION 쿠키를 쿠키자에 주입(로컬 연산, 블로킹 없음).
  //    이후 enroll 요청은 이 쿠키를 자동으로 실어 나른다.
  if (!cookieInjected) {
    const idx = (exec.vu.idInTest - 1) % data.cookies.length;
    const cookie = data.cookies[idx];
    if (!cookie) {
      // 이 VU 몫의 세션이 없으면(로그인 실패분) 신청을 건너뛴다.
      return;
    }
    http.cookieJar().set(BASE, 'SESSION', cookie);
    cookieInjected = true;
  }

  // ③ 수강 신청: X-Member-Id 는 넣지 않는다(게이트웨이가 세션의 memberId 로 주입).
  //    SESSION 쿠키는 쿠키자에 있으므로 자동으로 함께 전송된다.
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