-- ============================================================
-- LXP 더미 데이터 (테스트용)
-- schema.sql 기준 / FK 의존 순서대로 INSERT
-- ============================================================

-- ------------------------------------------------------------
-- Member (회원)
--   member_id 1~3 : 강사
--   member_id 4~7 : 수강생 (4번은 탈퇴 회원)
--   password_hash 는 BCrypt 형식 예시 (평문 'password1234')
-- ------------------------------------------------------------
INSERT INTO members (member_id, login_id, password_hash, nickname, status, created_at, deleted_at) VALUES
                                                                                                      (1, 'instructor_kim',  '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '김강사',   'ACTIVE',  '2026-01-02 09:00:00', NULL),
                                                                                                      (2, 'instructor_lee',  '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '이강사',   'ACTIVE',  '2026-01-03 09:00:00', NULL),
                                                                                                      (3, 'instructor_park', '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '박강사',   'ACTIVE',  '2026-01-04 09:00:00', NULL),
                                                                                                      (4, 'student_choi',    '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '최탈퇴',   'DELETED', '2026-01-05 09:00:00', '2026-05-01 12:00:00'),
                                                                                                      (5, 'test',    '$2a$10$1nBMyvTQfCPRBBmpPu2S7.0E.9CqDQyHXFkqi6TRHsmO6Q6cu2iWC', '정수강',   'ACTIVE',  '2026-02-10 09:00:00', NULL),
                                                                                                      (6, 'student_yoon',    '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '윤학생',   'ACTIVE',  '2026-02-11 09:00:00', NULL),
                                                                                                      (7, 'student_han',     '$2a$10$abcdefghijklmnopqrstuv1234567890ABCDEFGHIJKLMNOPq', '한열정',   'ACTIVE',  '2026-02-12 09:00:00', NULL);

-- ------------------------------------------------------------
-- Lecture (강의)
--   lecture_id 1~3 : PUBLIC (목록/상세 노출)
--   lecture_id 4   : PRIVATE (노출 제외)
--   lecture_id 5   : DELETED (노출 제외)
-- ------------------------------------------------------------
INSERT INTO lectures (lecture_id, instructor_id, title, description, status, created_at) VALUES
                                                                                             (1, 1, 'React 핵심 개념 완전 정복',     'useState, useEffect, Custom Hook 까지 React 기초를 다집니다.', 'PUBLIC',  '2026-02-01 10:00:00'),
                                                                                             (2, 1, '알고리즘 코딩테스트 입문',       'DP, 그래프, 그리디 기초 문제 풀이로 코테를 준비합니다.',       'PUBLIC',  '2026-02-05 10:00:00'),
                                                                                             (3, 2, 'Spring Boot REST API 설계',      'JPA 와 Spring Security 로 REST API 를 설계합니다.',           'PUBLIC',  '2026-02-08 10:00:00'),
                                                                                             (4, 2, '비공개 베타 강의',               '아직 공개되지 않은 강의입니다.',                               'PRIVATE', '2026-02-09 10:00:00'),
                                                                                             (5, 3, '삭제된 레거시 강의',             '폐강 처리된 강의입니다.',                                       'DELETED', '2026-01-15 10:00:00');

-- ------------------------------------------------------------
-- Content (콘텐츠)
--   PUBLIC 강의(1~3)에 콘텐츠, DELETED 강의(5)에도 1개 두어 노출 제외 검증
-- ------------------------------------------------------------
INSERT INTO contents (content_id, lecture_id, title, content_url, sort_order, created_at) VALUES
                                                                                              (1, 1, '1강. React 시작하기',            'https://cdn.lxp.test/react/01.mp4', 1, '2026-02-01 11:00:00'),
                                                                                              (2, 1, '2강. useState 다루기',           'https://cdn.lxp.test/react/02.mp4', 2, '2026-02-01 11:10:00'),
                                                                                              (3, 1, '3강. useEffect 와 생명주기',     'https://cdn.lxp.test/react/03.mp4', 3, '2026-02-01 11:20:00'),
                                                                                              (4, 2, '1강. 시간복잡도 이해',           'https://cdn.lxp.test/algo/01.mp4',  1, '2026-02-05 11:00:00'),
                                                                                              (5, 2, '2강. DP 기초',                   'https://cdn.lxp.test/algo/02.mp4',  2, '2026-02-05 11:10:00'),
                                                                                              (6, 3, '1강. REST 와 HTTP',              'https://cdn.lxp.test/spring/01.mp4',1, '2026-02-08 11:00:00'),
                                                                                              (7, 5, '폐강 강의 콘텐츠 (노출 제외)',   'https://cdn.lxp.test/legacy/01.mp4',1, '2026-01-15 11:00:00');

-- ------------------------------------------------------------
-- Enrollment (수강)
--   정수강(5) : 강의 1,2 수강 / 윤학생(6) : 강의 1 수강 / 한열정(7) : 강의 3 수강
--   UNIQUE(member_id, lecture_id) 중복 신청 방지 검증용 데이터 포함
-- ------------------------------------------------------------
INSERT INTO enrollments (enrollment_id, member_id, lecture_id, enrolled_at) VALUES
                                                                                (1, 5, 1, '2026-02-15 14:00:00'),
                                                                                (2, 5, 2, '2026-02-16 14:00:00'),
                                                                                (3, 6, 1, '2026-02-17 14:00:00'),
                                                                                (4, 7, 3, '2026-02-18 14:00:00');

-- ------------------------------------------------------------
-- LearningGoal (학습목표)
--   F02 화면 기준: 정수강(5)의 '오늘(2026-06-19)' 마감 목표 2개
--   + 어제 마감 목표 1개(오늘 조회 시 제외되는지 검증용)
--   + 윤학생(6) 목표 1개(본인 외 조회 차단 검증용)
-- ------------------------------------------------------------
INSERT INTO learning_goals (learning_goal_id, member_id, title, created_at, updated_at) VALUES
                                                                                                    (1, 5, 'React 핵심 개념 복습',   '2026-06-19 09:00:00', NULL),
                                                                                                    (2, 5, '코딩테스트 문제 2개 풀기', '2026-06-19 09:05:00', NULL),
                                                                                                    (3, 5, '어제 만료 목표(제외용)',   '2026-06-18 09:00:00', NULL),
                                                                                                    (4, 6, '윤학생 개인 목표',         '2026-06-19 08:00:00', NULL);

-- ------------------------------------------------------------
-- DetailGoal (세부목표)
--   학습목표 1 : 3개 중 1개 완료 (1/3 완료 — F02 화면과 동일)
--   학습목표 2 : 2개 중 0개 완료
--   학습목표 3,4 : 각 1개씩 (최소 1개 제약 충족)
-- ------------------------------------------------------------
INSERT INTO detail_goals (detail_goal_id, learning_goal_id, content, completed, sort_order) VALUES
                                                                                                (1, 1, 'useState / useEffect 다시 보기', TRUE,  1),
                                                                                                (2, 1, 'Custom Hook 직접 만들어보기',     FALSE, 2),
                                                                                                (3, 1, 'Context API 실습 예제 따라하기',  FALSE, 3),
                                                                                                (4, 2, '프로그래머스 LV2 그래프 탐색',    FALSE, 1),
                                                                                                (5, 2, '백준 DP 기초 문제',               FALSE, 2),
                                                                                                (6, 3, '어제 목표 세부항목',              FALSE, 1),
                                                                                                (7, 4, '윤학생 세부 목표',                FALSE, 1);

-- ------------------------------------------------------------
-- Question (질문)
--   PUBLIC / PRIVATE / DELETED 상태별로 조회 노출 검증
-- ------------------------------------------------------------
INSERT INTO questions (question_id, lecture_id, writer_id, title, content, visibility, status, created_at, updated_at, deleted_at) VALUES
                                                                                                                                       (1, 1, 5, 'useEffect 의존성 배열 질문', 'deps 에 빈 배열을 넣으면 언제 실행되나요?', 'PUBLIC',  'ACTIVE',  '2026-03-01 10:00:00', NULL, NULL),
                                                                                                                                       (2, 1, 6, '비공개 개인 질문',            '제 코드만 따로 봐주실 수 있나요?',          'PRIVATE', 'ACTIVE',  '2026-03-02 10:00:00', NULL, NULL),
                                                                                                                                       (3, 1, 5, '수정된 질문',                 '질문 내용을 수정했습니다.',                 'PUBLIC',  'ACTIVE',  '2026-03-03 10:00:00', '2026-03-03 12:00:00', NULL),
                                                                                                                                       (4, 2, 5, '삭제된 질문 (노출 제외)',     '잘못 올린 질문입니다.',                     'PUBLIC',  'DELETED', '2026-03-04 10:00:00', NULL, '2026-03-05 09:00:00');

-- ------------------------------------------------------------
-- Review (후기)
--   수강한 회원만 작성 / ACTIVE,HIDDEN,DELETED 상태별 조회 검증
--   강의 1 : 정수강(5)·윤학생(6) 후기 / 강의 2 : 정수강(5)
-- ------------------------------------------------------------
INSERT INTO reviews (review_id, lecture_id, writer_id, content, rating, status, created_at) VALUES
                                                                                                (1, 1, 5, '개념 설명이 정말 깔끔해서 이해가 잘 됐어요.', 5, 'ACTIVE',  '2026-03-10 10:00:00'),
                                                                                                (2, 1, 6, 'Custom Hook 파트가 특히 도움됐습니다.',      4, 'ACTIVE',  '2026-03-11 10:00:00'),
                                                                                                (3, 2, 5, 'DP 문제 풀이가 단계별로 좋았어요.',          5, 'ACTIVE',  '2026-03-12 10:00:00'),
                                                                                                (4, 1, 5, '숨김 처리된 후기 (노출 제외)',               2, 'HIDDEN',  '2026-03-13 10:00:00'),
                                                                                                (5, 1, 6, '삭제된 후기 (노출 제외)',                    1, 'DELETED', '2026-03-14 10:00:00');

-- ------------------------------------------------------------
-- ReviewLike (후기좋아요)
--   review 1 : 윤학생(6)·한열정(7) → 좋아요 2개
--   review 2 : 정수강(5) → 좋아요 1개
--   review 3 : 좋아요 0개 (likeCount 계산 = 0 검증)
--   UNIQUE(review_id, member_id) 중복 방지 검증용
-- ------------------------------------------------------------
INSERT INTO review_likes (review_like_id, review_id, member_id, created_at) VALUES
                                                                                (1, 1, 6, '2026-03-15 10:00:00'),
                                                                                (2, 1, 7, '2026-03-15 11:00:00'),
                                                                                (3, 2, 5, '2026-03-16 10:00:00');