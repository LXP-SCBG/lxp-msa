import { apiRequest } from "@/lib/api/client";
import type { Goal, GoalCreateRequest, GoalUpdateRequest } from "@/types/goal";

/** 오늘의 목표 목록 조회 (생성 후 24h 이내) */
export function getGoals() {
  return apiRequest<Goal[]>("/goals");
}

/** 학습목표 등록 */
export function createGoal(body: GoalCreateRequest) {
  return apiRequest<Goal>("/goals", { method: "POST", body });
}

/** 학습목표 수정 (제목 + 세부목표 동기화) */
export function updateGoal(goalId: number, body: GoalUpdateRequest) {
  return apiRequest<Goal>(`/goals/${goalId}`, { method: "PATCH", body });
}

/** 학습목표 삭제 (soft delete) */
export function deleteGoal(goalId: number) {
  return apiRequest<void>(`/goals/${goalId}`, { method: "DELETE" });
}

/** 세부목표 달성 상태 토글. 응답으로 목표 전체(completed 포함) 최신 상태를 받는다. */
export function toggleDetailCompletion(
  goalId: number,
  detailGoalId: number,
  completed: boolean,
) {
  return apiRequest<Goal>(
    `/goals/${goalId}/details/${detailGoalId}/completion`,
    { method: "PATCH", body: { completed } },
  );
}
