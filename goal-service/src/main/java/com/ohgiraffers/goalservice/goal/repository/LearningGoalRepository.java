package com.ohgiraffers.goalservice.goal.repository;

import com.ohgiraffers.goalservice.goal.domain.LearningGoal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningGoalRepository extends JpaRepository<LearningGoal, Long> {

    /** 특정 회원의 목표 중 생성 시각이 기준 시각 이후(=만료 전)인 것을 생성순으로 조회한다. */
    List<LearningGoal> findByMemberIdAndCreatedAtAfterOrderByCreatedAtAsc(
            Long memberId, LocalDateTime threshold);
}
