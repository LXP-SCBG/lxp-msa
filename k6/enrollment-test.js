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
  // 서버가 느려져도 부하를 유지하므로 "몰림" 상황을 정확히 재현한다.
  // 총 3분 지속. 평상시(5/s) 흐르다가 약 3,000명 규모 스파이크가 2번 발생한다.
  // 정원 1000석 → 스파이크로 좌석을 쟁탈하다 만석이 되면 이후는 전부 409로 방어.
  scenarios: {
    strike: {
      executor: 'ramping-arrival-rate',
      startRate: 5,           // 평상시 5 req/s 로 시작
      timeUnit: '1s',
      preAllocatedVUs: 200,   // 미리 확보해 둘 VU
      maxVUs: 3500,           // 스파이크 피크에서 필요한 최대 VU(락 대기로 오래 잡힘)
      stages: [
        { target: 5,    duration: '10s' }, // 평상시 흐름
        { target: 300,  duration: '2s'  }, // ⚡ 1차 급증 — 약 300명 규모로 튐
        { target: 300,  duration: '3s'  }, // 피크 유지: 좌석 쟁탈(201 다수 발생)
        { target: 5,    duration: '5s'  }, // 진정
        { target: 5,    duration: '70s' }, // 평상시 지속
        { target: 3000, duration: '2s'  }, // ⚡ 2차 급증 — 다시 몰림
        { target: 3000, duration: '3s'  }, // 피크 유지: 남은 좌석 쟁탈 → 만석 후 409 방어
        { target: 5,    duration: '5s'  }, // 진정
        { target: 5,    duration: '80s' }, // 마무리 평상시(회복/안정성 확인)
      ],
    },
  },
  thresholds: {
    // 5xx/타임아웃/400 등 "진짜 실패"만 집계(201·409는 위 콜백으로 정상 처리).
    'http_req_failed': ['rate<0.01'],
    // 스파이크 구간엔 락 큐잉으로 지연이 치솟는다. 넘기면 그 자체가 관찰 포인트.
    'http_req_duration': ['p(95)<3000'],
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
