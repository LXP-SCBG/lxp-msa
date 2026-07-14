package com.ohgiraffers.goalservice.goal.dto;

import java.util.List;

public record GoalCreateRequest(
        String title,
        List<DetailGoalItem> detailGoals
) {

    public record DetailGoalItem(
            String content
    ) {
    }
}
