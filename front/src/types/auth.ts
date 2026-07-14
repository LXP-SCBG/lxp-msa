export interface LoginRequest {
  loginId: string;
  password: string;
}

export interface SignupRequest {
  loginId: string;
  password: string;
  nickname: string;
}

export interface MemberResponse {
  memberId: number;
  loginId: string;
  nickname: string;
}

export interface ApiErrorResponse {
  message: string;
  fieldErrors?: Record<string, string>;
}
