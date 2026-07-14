package com.ohgiraffers.goalservice.goal.controller;

import com.ohgiraffers.goalservice.common.auth.LoginMember;
import com.ohgiraffers.goalservice.goal.dto.CompletionUpdateRequest;
import com.ohgiraffers.goalservice.goal.dto.GoalCreateRequest;
import com.ohgiraffers.goalservice.goal.dto.GoalCreateResponse;
import com.ohgiraffers.goalservice.goal.dto.GoalResponse;
import com.ohgiraffers.goalservice.goal.dto.GoalUpdateRequest;
import com.ohgiraffers.goalservice.goal.service.GoalService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalCreateResponse> createGoal(
            @LoginMember Long memberId,
            @RequestBody GoalCreateRequest request
    ) {
        return ResponseEntity.ok(goalService.createGoal(memberId, request));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> findTodayGoals(
            @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(goalService.findTodayGoals(memberId));
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @LoginMember Long memberId,
            @PathVariable Long goalId,
            @RequestBody GoalUpdateRequest request
    ) {
        return ResponseEntity.ok(goalService.updateGoal(memberId, goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @LoginMember Long memberId,
            @PathVariable Long goalId
    ) {
        goalService.deleteGoal(memberId, goalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{goalId}/details/{detailGoalId}/completion")
    public ResponseEntity<GoalResponse> updateDetailGoalCompletion(
            @LoginMember Long memberId,
            @PathVariable Long goalId,
            @PathVariable Long detailGoalId,
            @RequestBody CompletionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                goalService.updateDetailGoalCompletion(memberId, goalId, detailGoalId, request.completed()));
    }
}
