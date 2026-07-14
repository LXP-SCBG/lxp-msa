package com.ohgiraffers.goalservice.goal.repository;

import com.ohgiraffers.goalservice.goal.domain.DetailGoal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetailGoalRepository extends JpaRepository<DetailGoal, Long> {

    List<DetailGoal> findByLearningGoalIdOrderBySortOrderAsc(Long learningGoalId);

    /** 여러 학습목표의 세부목표를 한 번에 조회한다. (N+1 방지) */
    List<DetailGoal> findByLearningGoalIdInOrderBySortOrderAsc(List<Long> learningGoalIds);
}
