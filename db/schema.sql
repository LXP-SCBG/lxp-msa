-- ============================================================
-- LXP DDL
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- 기존 테이블 삭제 (재실행 대비)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS review_likes;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS questions;
DROP TABLE IF EXISTS detail_goals;
DROP TABLE IF EXISTS learning_goals;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS contents;
DROP TABLE IF EXISTS lectures;
DROP TABLE IF EXISTS members;

-- ------------------------------------------------------------
-- Member (회원)
-- ------------------------------------------------------------
CREATE TABLE members (
                         member_id     BIGINT       NOT NULL AUTO_INCREMENT,
                         login_id      VARCHAR(100) NOT NULL,
                         password_hash VARCHAR(255) NOT NULL,
                         nickname      VARCHAR(100) NOT NULL,
                         status        VARCHAR(20)  NOT NULL,
                         role          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
                         created_at    DATETIME     NOT NULL,
                         deleted_at    DATETIME     NULL,

                         PRIMARY KEY (member_id),
                         UNIQUE KEY uq_member_login_id (login_id),
                         CONSTRAINT chk_member_status CHECK (status IN ('ACTIVE', 'DELETED')),
                         CONSTRAINT chk_member_role CHECK (role IN ('STUDENT', 'INSTRUCTOR'))
);

-- ------------------------------------------------------------
-- Lecture (강의)
-- ------------------------------------------------------------
CREATE TABLE lectures (
                          lecture_id    BIGINT       NOT NULL AUTO_INCREMENT,
                          instructor_id BIGINT       NOT NULL,
                          title         VARCHAR(255) NOT NULL,
                          description   TEXT         NULL,
                          status        VARCHAR(20)  NOT NULL,
                          created_at    DATETIME     NOT NULL,

                          PRIMARY KEY (lecture_id),
                          CONSTRAINT fk_lecture_instructor FOREIGN KEY (instructor_id) REFERENCES members (member_id),
                          CONSTRAINT chk_lecture_status CHECK (status IN ('PUBLIC', 'PRIVATE', 'DELETED'))
);

-- ------------------------------------------------------------
-- Content (콘텐츠)
-- ------------------------------------------------------------
CREATE TABLE contents (
                          content_id  BIGINT       NOT NULL AUTO_INCREMENT,
                          lecture_id  BIGINT       NOT NULL,
                          title       VARCHAR(255) NOT NULL,
                          content_url VARCHAR(500) NOT NULL,
                          sort_order  INT          NOT NULL,
                          created_at  DATETIME     NOT NULL,

                          PRIMARY KEY (content_id),
                          CONSTRAINT fk_content_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (lecture_id)
);

-- ------------------------------------------------------------
-- Enrollment (수강)
-- ------------------------------------------------------------
CREATE TABLE enrollments (
                             enrollment_id BIGINT   NOT NULL AUTO_INCREMENT,
                             member_id     BIGINT   NOT NULL,
                             lecture_id    BIGINT   NOT NULL,
                             enrolled_at   DATETIME NOT NULL,

                             PRIMARY KEY (enrollment_id),
                             UNIQUE KEY uq_enrollment (member_id, lecture_id),
                             CONSTRAINT fk_enrollment_member  FOREIGN KEY (member_id)  REFERENCES members (member_id),
                             CONSTRAINT fk_enrollment_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (lecture_id)
);

-- ------------------------------------------------------------
-- LearningGoal (학습목표)
-- ------------------------------------------------------------
CREATE TABLE learning_goals (
                                learning_goal_id BIGINT      NOT NULL AUTO_INCREMENT,
                                member_id        BIGINT      NOT NULL,
                                title            VARCHAR(30) NOT NULL,
                                created_at       DATETIME    NOT NULL,
                                updated_at       DATETIME    NULL,
                                deleted_at       DATETIME    NULL,

                                PRIMARY KEY (learning_goal_id),
                                CONSTRAINT fk_learning_goal_member FOREIGN KEY (member_id) REFERENCES members (member_id),
                                CONSTRAINT chk_learning_goal_title_len CHECK (CHAR_LENGTH(title) <= 30)
);

-- ------------------------------------------------------------
-- DetailGoal (세부목표)
-- ------------------------------------------------------------
CREATE TABLE detail_goals (
                              detail_goal_id   BIGINT       NOT NULL AUTO_INCREMENT,
                              learning_goal_id BIGINT       NOT NULL,
                              content          VARCHAR(100) NOT NULL,
                              completed        BOOLEAN      NOT NULL DEFAULT FALSE,
                              sort_order       INT          NOT NULL,
                              deleted_at       DATETIME     NULL,

                              PRIMARY KEY (detail_goal_id),
                              CONSTRAINT fk_detail_goal_learning_goal FOREIGN KEY (learning_goal_id) REFERENCES learning_goals (learning_goal_id),
                              CONSTRAINT chk_detail_goal_content_len CHECK (CHAR_LENGTH(content) <= 100)
);

-- ------------------------------------------------------------
-- Question (질문)
-- ------------------------------------------------------------
CREATE TABLE questions (
                           question_id BIGINT       NOT NULL AUTO_INCREMENT,
                           lecture_id  BIGINT       NOT NULL,
                           writer_id   BIGINT       NOT NULL,
                           title       VARCHAR(255) NOT NULL,
                           content     TEXT         NOT NULL,
                           visibility  VARCHAR(20)  NOT NULL,
                           status      VARCHAR(20)  NOT NULL,
                           created_at  DATETIME     NOT NULL,
                           updated_at  DATETIME     NULL,
                           deleted_at  DATETIME     NULL,

                           PRIMARY KEY (question_id),
                           CONSTRAINT fk_question_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (lecture_id),
                           CONSTRAINT fk_question_writer  FOREIGN KEY (writer_id)  REFERENCES members (member_id),
                           CONSTRAINT chk_question_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
                           CONSTRAINT chk_question_status     CHECK (status IN ('ACTIVE', 'DELETED'))
);

-- ------------------------------------------------------------
-- Review (후기)
-- ------------------------------------------------------------
CREATE TABLE reviews (
                         review_id  BIGINT      NOT NULL AUTO_INCREMENT,
                         lecture_id BIGINT      NOT NULL,
                         writer_id  BIGINT      NOT NULL,
                         content    VARCHAR(50) NOT NULL,
                         rating     INT         NOT NULL,
                         status     VARCHAR(20) NOT NULL,
                         created_at DATETIME    NOT NULL,

                         PRIMARY KEY (review_id),
                         CONSTRAINT fk_review_lecture FOREIGN KEY (lecture_id) REFERENCES lectures (lecture_id),
                         CONSTRAINT fk_review_writer  FOREIGN KEY (writer_id)  REFERENCES members (member_id),
                         CONSTRAINT chk_review_status CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED')),
                         CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
                         CONSTRAINT chk_review_content_len CHECK (CHAR_LENGTH(content) >= 1 AND CHAR_LENGTH(content) < 50)
);

-- ------------------------------------------------------------
-- ReviewLike (후기좋아요)
-- ------------------------------------------------------------
CREATE TABLE review_likes (
                              review_like_id BIGINT   NOT NULL AUTO_INCREMENT,
                              review_id      BIGINT   NOT NULL,
                              member_id      BIGINT   NOT NULL,
                              created_at     DATETIME NOT NULL,

                              PRIMARY KEY (review_like_id),
                              UNIQUE KEY uq_review_like (review_id, member_id),
                              CONSTRAINT fk_review_like_review FOREIGN KEY (review_id) REFERENCES reviews (review_id),
                              CONSTRAINT fk_review_like_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);

SET FOREIGN_KEY_CHECKS = 1;