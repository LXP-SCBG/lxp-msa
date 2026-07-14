import { LoginRequest, SignupRequest, MemberResponse, ApiErrorResponse } from "@/types/auth";

export class SignupApiError extends Error {
  status: number;
  fieldErrors?: Record<string, string>;

  constructor(message: string, status: number, fieldErrors?: Record<string, string>) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

const BASE_URL = process.env.NEXT_PUBLIC_API_BASIC_URL ?? "http://localhost:8080";

export async function loginApi(body: LoginRequest): Promise<MemberResponse> {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    throw new Error("로그인 정보가 일치하지 않습니다.");
  }

  return res.json() as Promise<MemberResponse>;
}

export async function signupApi(body: SignupRequest): Promise<MemberResponse> {
  const res = await fetch(`${BASE_URL}/members`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const data: ApiErrorResponse = await res.json();
    throw new SignupApiError(data.message, res.status, data.fieldErrors);
  }

  return res.json() as Promise<MemberResponse>;
}

/**
 * 현재 로그인한 회원 조회 (세션 복원용).
 * 비로그인(401) 등 실패 시 null 을 반환해 "로그아웃 상태"로 취급한다.
 */
export async function getMeApi(): Promise<MemberResponse | null> {
  const res = await fetch(`${BASE_URL}/auth/me`, {
    credentials: "include",
  });

  if (!res.ok) {
    return null;
  }
  return res.json() as Promise<MemberResponse>;
}

export async function logoutApi(): Promise<void> {
  const res = await fetch(`${BASE_URL}/auth/logout`, {
    method: "POST",
    credentials: "include",
  });

  if (!res.ok) {
    throw new Error("로그아웃에 실패했습니다. 다시 시도해 주세요.");
  }
}
