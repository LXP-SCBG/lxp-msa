import type {
  ErrorResponse,
  ReviewCreateRequest,
  ReviewLikeResponse,
  ReviewListResponse,
  ReviewResponse,
} from "@/types/review";

const API_BASE_URL = (
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  (process.env.NODE_ENV === "development" ? "http://localhost:8080" : "")
).replace(/\/$/, "");


const REQUEST_TIMEOUT_MS = 8000;

function buildApiUrl(path: string) {
  return `${API_BASE_URL}${path}`;
}

async function fetchWithTimeout(
  input: string,
  init: RequestInit
): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    return await fetch(input, {
      ...init,
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timeoutId);
  }
}

async function getErrorMessage(response: Response, fallbackMessage: string) {
  try {
    const errorBody = (await response.json()) as ErrorResponse;

    if (errorBody.fieldErrors) {
      const firstFieldError = Object.values(errorBody.fieldErrors)[0];

      if (firstFieldError) {
        return firstFieldError;
      }
    }

    if (typeof errorBody.message === "string") {
      return errorBody.message;
    }

    return fallbackMessage;
  } catch {
    return fallbackMessage;
  }
}

export async function getReviews(
  lectureId: number
): Promise<ReviewListResponse[]> {
  let response: Response;

  try {
    response = await fetchWithTimeout(
      buildApiUrl(`/lectures/${lectureId}/reviews`),
      {
        method: "GET",
        credentials: "include",
        cache: "no-store",
      }
    );
  } catch {
    throw new Error("후기 목록을 불러오지 못했습니다.");
  }

  if (!response.ok) {
    const message = await getErrorMessage(
      response,
      "후기 목록을 불러오지 못했습니다."
    );

    throw new Error(message);
  }

  return response.json();
}

export async function createReview(
  lectureId: number,
  data: ReviewCreateRequest
): Promise<ReviewResponse> {
  let response: Response;

  try {
    response = await fetchWithTimeout(
      buildApiUrl(`/lectures/${lectureId}/reviews`),
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
      }
    );
  } catch {
    throw new Error("후기 등록에 실패했습니다.");
  }

  if (!response.ok) {
    const message = await getErrorMessage(
      response,
      "후기 등록에 실패했습니다."
    );

    throw new Error(message);
  }

  return response.json();
}

export async function likeReview(
  reviewId: number
): Promise<ReviewLikeResponse> {
  let response: Response;

  try {
    response = await fetchWithTimeout(
      buildApiUrl(`/reviews/${reviewId}/likes`),
      {
        method: "POST",
        credentials: "include",
      }
    );
  } catch {
    throw new Error("좋아요 등록에 실패했습니다.");
  }

  if (!response.ok) {
    const message = await getErrorMessage(
      response,
      "좋아요 등록에 실패했습니다."
    );

    throw new Error(message);
  }

  return response.json();
}