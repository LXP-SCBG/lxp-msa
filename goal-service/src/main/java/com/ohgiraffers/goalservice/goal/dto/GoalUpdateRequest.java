package com.ohgiraffers.goalservice.goal.dto;

import java.util.List;


public record GoalUpdateRequest(
        String title,
        List<DetailGoalItem> detailGoals
) {

    public record DetailGoalItem(
            Long detailGoalId,
            String content
    ) {
    }
}
