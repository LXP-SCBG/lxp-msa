// 백엔드 공통 fetch 래퍼.
// - 세션 쿠키(JSESSIONID)를 주고받기 위해 credentials: "include" 고정
// - 에러 응답({ message })을 ApiError 로 변환해 호출부에서 status/메시지로 분기 가능

const BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

type RequestOptions = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
};

export async function apiRequest<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { method = "GET", body } = options;

  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    credentials: "include", // 세션 쿠키 전송 (없으면 로그인 상태가 안 잡힘)
    headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!res.ok) {
    let message = "요청 처리 중 오류가 발생했습니다.";
    try {
      const data = await res.json();
      if (data?.message) message = data.message;
    } catch {
      // 본문이 없거나 JSON이 아닌 경우 기본 메시지 사용
    }
    throw new ApiError(res.status, message);
  }

  if (res.status === 204) {
    return undefined as T; // No Content (삭제 등)
  }
  return res.json() as Promise<T>;
}
