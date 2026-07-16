package com.ohgiraffers.goalservice.goal.controller;

import com.ohgiraffers.goalservice.goal.dto.*;
import com.ohgiraffers.goalservice.goal.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<GoalCreateResponse> createGoal(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody GoalCreateRequest request
    ) {
        return ResponseEntity.ok(goalService.createGoal(memberId, request));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> findTodayGoals(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        return ResponseEntity.ok(goalService.findTodayGoals(memberId));
    }

    @PatchMapping("/{goalId}")
    public ResponseEntity<GoalResponse> updateGoal(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long goalId,
            @RequestBody GoalUpdateRequest request
    ) {
        return ResponseEntity.ok(goalService.updateGoal(memberId, goalId, request));
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long goalId
    ) {
        goalService.deleteGoal(memberId, goalId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{goalId}/details/{detailGoalId}/completion")
    public ResponseEntity<GoalResponse> updateDetailGoalCompletion(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long goalId,
            @PathVariable Long detailGoalId,
            @RequestBody CompletionUpdateRequest request
    ) {
        return ResponseEntity.ok(
                goalService.updateDetailGoalCompletion(memberId, goalId, detailGoalId, request.completed()));
    }
}
