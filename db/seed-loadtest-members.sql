-- ============================================================
-- 부하테스트 전용 회원 시드 — member_id 1000~12999 (12,000명)
--
-- k6/enrollment-test.js 가 BASE_MEMBER_ID(1000)~MEMBER_POOL(12000)
-- 범위의 회원 id로 "게이트웨이 로그인 → 수강 신청"을 쏘기 위해 필요한
-- 활성 회원 더미 데이터. data.sql(기능 테스트용 소규모 시드)과 분리해서
-- 관리한다 — 매 스키마 리셋(schema.sql + data.sql)마다 이 파일도 다시
-- 실행해야 재적용된다.
--
-- password_hash는 임의 문자열이 아니라 반드시 유효한 BCrypt 해시여야 한다.
-- 게이트웨이를 거치는 부하테스트는 각 회원으로 실제 로그인(POST /auth/login)
-- 을 해야 세션에 memberId가 실리고, 그래야 MemberIdRelayFilter가
-- X-Member-Id 헤더를 채워 보낸다. BCrypt 해시는 계정마다 다를 필요 없이
-- 같은 평문(password1234)에 대해 항상 검증되므로 12,000명 전원이 같은
-- 비밀번호를 공유해도 무방하다.
--
-- 주의: data.sql의 'test'(member_id 5) 계정 해시
-- ($2a$10$1nBMyvTQfCPRBBmpPu2S7.0E.9CqDQyHXFkqi6TRHsmO6Q6cu2iWC)는 주석과
-- 달리 실제로는 'password1234'와 매치되지 않는 것으로 확인됐다(직접 로그인
-- 시도 시 401). 아래 해시는 python bcrypt로 새로 생성해 로그인 성공까지
-- 검증한 값이다.
--
-- 실행(최초 삽입/재적용 모두 동일 — 이미 있으면 password_hash만 갱신):
--   docker exec -i lxp-msa-mysql-1 mysql -usohee -psohee LXP < db/seed-loadtest-members.sql
-- ============================================================

SET SESSION cte_max_recursion_depth = 20000;

INSERT INTO members (member_id, login_id, password_hash, nickname, status, created_at)
WITH RECURSIVE seq AS (
    SELECT 1000 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 12999
)
SELECT
    n,
    CONCAT('loadtest_', n),
    -- 'password1234' 의 유효한 BCrypt 해시 (로그인 성공까지 검증됨)
    '$2b$10$IOObN5M7VOGvargPEy0Ri.DhDinQ.t8P91kVl5UXg9Wcg6TD2TUhq',
    CONCAT('부하테스트', n),
    'ACTIVE',
    NOW()
FROM seq
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash);
