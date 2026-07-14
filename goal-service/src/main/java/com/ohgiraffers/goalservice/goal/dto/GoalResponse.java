package com.ohgiraffers.goalservice.goal.dto;

import com.ohgiraffers.goalservice.goal.domain.DetailGoal;
import com.ohgiraffers.goalservice.goal.domain.LearningGoal;
import java.time.LocalDateTime;
import java.util.List;


public record GoalResponse(
        Long learningGoalId,
        String title,
        boolean completed,
        LocalDateTime createdAt,
        List<DetailGoalItem> detailGoals
) {

    public static GoalResponse of(LearningGoal learningGoal, List<DetailGoal> detailGoals) {
        return new GoalResponse(
                learningGoal.getLearningGoalId(),
                learningGoal.getTitle(),
                isAllCompleted(detailGoals),  // 세부목표가 전부 완료되면 목표 달성으로 본다
                learningGoal.getCreatedAt(),
                detailGoals.stream()
                        .map(DetailGoalItem::from)
                        .toList()
        );
    }

    /** 세부목표가 1개 이상이면서 전부 완료됐는지 여부. */
    private static boolean isAllCompleted(List<DetailGoal> detailGoals) {
        return !detailGoals.isEmpty()
                && detailGoals.stream().allMatch(DetailGoal::isCompleted);
    }

    public record DetailGoalItem(
            Long detailGoalId,
            String content,
            boolean completed,
            int sortOrder
    ) {

        public static DetailGoalItem from(DetailGoal detailGoal) {
            return new DetailGoalItem(
                    detailGoal.getDetailGoalId(),
                    detailGoal.getContent(),
                    detailGoal.isCompleted(),
                    detailGoal.getSortOrder()
            );
        }
    }
}
