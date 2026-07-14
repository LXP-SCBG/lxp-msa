export type QuestionVisibility = "PUBLIC" | "PRIVATE";

export interface QuestionListResponse {
  questionId: number;
  title: string;
  writerNickname: string;
  createdAt: string;
  visibility: QuestionVisibility;
}

export interface QuestionDetailResponse {
  questionId: number;
  writerId: number;
  title: string;
  content: string;
  writerNickname: string;
  createdAt: string;
  visibility: QuestionVisibility;
}