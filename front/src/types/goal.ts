// 백엔드 goal 도메인 DTO와 1:1로 맞춘 타입.

/** 세부목표 (백엔드 GoalResponse.DetailGoalItem) */
export interface DetailGoal {
  detailGoalId: number;
  content: string;
  completed: boolean;
  sortOrder: number;
}

/** 학습목표 (백엔드 GoalResponse). completed = 세부목표 전부 완료 여부(목표 달성) */
export interface Goal {
  learningGoalId: number;
  title: string;
  completed: boolean;
  createdAt: string; // ISO LocalDateTime 문자열 (예: "2026-06-19T09:00:00")
  detailGoals: DetailGoal[];
}

/** 등록 요청 (POST /goals). sortOrder는 서버가 배열 순서대로 자동 부여 */
export interface GoalCreateRequest {
  title: string;
  detailGoals: { content: string }[];
}

/**
 * 수정 요청 (PATCH /goals/{goalId}).
 * detailGoalId 가 있으면 기존 항목 수정, null 이면 신규 추가, 요청에서 빠지면 삭제.
 */
export interface GoalUpdateRequest {
  title: string;
  detailGoals: { detailGoalId: number | null; content: string }[];
}
