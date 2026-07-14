"use client";

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { MemberResponse } from "@/types/auth";
import { getMeApi } from "@/lib/api/auth";

interface AuthState {
  member: MemberResponse | null;
  setMember: (member: MemberResponse | null) => void;
  /** 세션 복원(GET /auth/me) 진행 중 여부. true 동안은 로그인/로그아웃 UI를 확정하지 않는다. */
  loading: boolean;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [member, setMember] = useState<MemberResponse | null>(null);
  const [loading, setLoading] = useState(true);

  // 새로고침 시 메모리 상태가 초기화되므로, 마운트 때 서버 세션으로 로그인 상태를 복원한다.
  useEffect(() => {
    let active = true;
    getMeApi()
      .then((m) => {
        if (active) setMember(m);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <AuthContext.Provider value={{ member, setMember, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuthContext(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuthContext must be used within AuthProvider");
  }
  return ctx;
}
