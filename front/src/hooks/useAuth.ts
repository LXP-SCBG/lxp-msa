import { useState } from "react";
import { useRouter } from "next/navigation";
import { loginApi, logoutApi } from "@/lib/api/auth";
import { LoginRequest } from "@/types/auth";
import { useAuthContext } from "@/store/authStore";

export function useAuth() {
  const { setMember } = useAuthContext();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [isPending, setIsPending] = useState(false);

  async function login(body: LoginRequest) {
    setError(null);
    setIsPending(true);
    try {
      const member = await loginApi(body);
      setMember(member);
      router.push("/lectures");
    } catch {
      setError("로그인 정보가 일치하지 않습니다.");
    } finally {
      setIsPending(false);
    }
  }

  async function logout() {
    setIsPending(true);
    try {
      await logoutApi();
    } catch {
      // 세션 무효화 실패해도 클라이언트 상태는 초기화
    } finally {
      setMember(null);
      setIsPending(false);
      router.push("/lectures");
    }
  }

  return { login, logout, error, isPending };
}
