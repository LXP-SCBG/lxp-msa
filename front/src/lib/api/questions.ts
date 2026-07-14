import { apiRequest, ApiError } from "@/lib/api/client";
import type {
  QuestionListResponse,
  QuestionDetailResponse,
} from "@/types/question";

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

// 조회 2개는 body가 있으니 apiRequest 그대로
export function getQuestions(lectureId: number) {
  return apiRequest<QuestionListResponse[]>(`/questions?lectureId=${lectureId}`);
}

export function getQuestion(questionId: number) {
  return apiRequest<QuestionDetailResponse>(`/questions/${questionId}`);
}

// 등록은 questionId(body) 반환하니 apiRequest 그대로
export function createQuestion(body: {
  lectureId: number;
  title: string;
  content: string;
  isPublic: boolean;
}) {
  return apiRequest<number>("/questions", { method: "POST", body });
}

// 수정·삭제는 응답 body가 없어 직접 처리 (빈 body JSON 파싱 회피)
export async function updateQuestion(
  questionId: number,
  body: { title?: string; content?: string },
) {
  const res = await fetch(`${BASE_URL}/questions/${questionId}`, {
    method: "PATCH",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    let message = "요청 처리 중 오류가 발생했습니다.";
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      // body 없으면 기본 메시지
    }
    throw new ApiError(res.status, message);
  }
}

export async function deleteQuestion(questionId: number) {
  const res = await fetch(`${BASE_URL}/questions/${questionId}`, {
    method: "DELETE",
    credentials: "include",
  });
  if (!res.ok) {
    let message = "요청 처리 중 오류가 발생했습니다.";
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      // body 없으면 기본 메시지
    }
    throw new ApiError(res.status, message);
  }
}