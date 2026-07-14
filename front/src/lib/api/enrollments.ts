import { apiRequest, ApiError } from "@/lib/api/client";
import type { EnrollRequest, EnrollmentResponse } from "@/types/enrollment";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ??
  "http://localhost:8080";

async function getErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const data = await response.json();

    if (typeof data?.message === "string") {
      return data.message;
    }

    return fallbackMessage;
  } catch {
    return fallbackMessage;
  }
}

export function getEnrollments(): Promise<EnrollmentResponse[]> {
  return apiRequest<EnrollmentResponse[]>("/enrollments");
}

export async function enrollLecture(lectureId: number): Promise<void> {
  const body: EnrollRequest = { lectureId };

  let response: Response;

  try {
    response = await fetch(`${API_BASE_URL}/enrollments`, {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(body),
    });
  } catch {
    throw new Error("수강 신청에 실패했습니다.");
  }

  if (!response.ok) {
    const message = await getErrorMessage(
      response,
      "수강 신청에 실패했습니다."
    );

    throw new ApiError(response.status, message);
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}