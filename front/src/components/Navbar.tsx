"use client";

import Link from "next/link";
import { useAuthContext } from "@/store/authStore";
import { useAuth } from "@/hooks/useAuth";

export default function Navbar() {
  const { member, loading } = useAuthContext();
  const { logout, isPending } = useAuth();

  return (
    <nav className="fixed top-0 left-0 right-0 z-50 h-14 bg-white border-b border-gray-100 flex items-center px-8">
      {/* Logo */}
      <Link href="/lectures" className="flex items-center gap-2 mr-8">
        <span className="w-8 h-8 bg-violet-600 rounded-lg flex items-center justify-center text-white font-bold text-sm select-none">
          L
        </span>
        <span className="font-bold text-lg tracking-tight">Loop.</span>
      </Link>

      {/* Center nav links */}
      <div className="flex items-center gap-6 flex-1">
        <Link
          href="/lectures"
          className="text-sm text-gray-600 hover:text-gray-900 transition-colors"
        >
          강의 둘러보기
        </Link>
      </div>

      {/* Right: 로그인 상태에 따라 분기 (loading 동안은 깜빡임 방지로 비워둠) */}
      {loading ? (
        <div className="w-24 h-8" aria-hidden />
      ) : member ? (
        <div className="flex items-center gap-3">
          <span className="text-sm text-gray-600">{member.nickname}</span>
          <button
            onClick={logout}
            disabled={isPending}
            className="inline-flex items-center justify-center px-4 py-1.5 rounded-lg border border-gray-300 text-gray-600 text-sm font-medium bg-white hover:bg-gray-50 transition-colors disabled:opacity-50"
          >
            로그아웃
          </button>
        </div>
      ) : (
        <Link
          href="/login"
          className="inline-flex items-center justify-center px-4 py-1.5 rounded-lg border border-violet-600 text-violet-600 text-sm font-medium bg-white hover:bg-violet-50 transition-colors"
        >
          로그인
        </Link>
      )}
    </nav>
  );
}
