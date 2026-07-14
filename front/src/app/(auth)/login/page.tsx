"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { useAuth } from "@/hooks/useAuth";

export default function LoginPage() {
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const { login, error, isPending } = useAuth();

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    await login({ loginId, password });
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-1">로그인</h1>
          <p className="text-sm text-gray-500">Loop에서 학습을 이어가세요.</p>
        </div>

        {/* Form */}
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Input
            label="아이디"
            id="user-id"
            type="text"
            placeholder="phb123"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
          />
          <Input
            label="비밀번호"
            id="password"
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && (
            <p className="text-sm text-red-500">{error}</p>
          )}

          <Button type="submit" fullWidth className="mt-2 py-3" disabled={isPending}>
            {isPending ? "로그인 중..." : "로그인"}
          </Button>
        </form>

        {/* Footer */}
        <p className="mt-4 text-center text-sm text-gray-500">
          아직 회원이 아니신가요?{" "}
          <Link
            href="/register"
            className="text-violet-600 font-medium hover:underline"
          >
            회원가입
          </Link>
        </p>
      </div>
    </div>
  );
}
