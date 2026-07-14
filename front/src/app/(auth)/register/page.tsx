"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { signupApi, SignupApiError } from "@/lib/api/auth";
import { useAuthContext } from "@/store/authStore";

type FieldErrors = {
  loginId?: string;
  password?: string;
  nickname?: string;
};

function validateLoginId(value: string): string | undefined {
  if (!value) return "아이디를 입력해 주세요.";
  if (!/^[a-zA-Z0-9]+$/.test(value)) return "아이디는 영문과 숫자만 사용할 수 있습니다.";
  if (value.length < 4 || value.length > 20) return "아이디는 4~20자로 입력해 주세요.";
}

function validatePassword(value: string): string | undefined {
  if (!value) return "비밀번호를 입력해 주세요.";
  if (value.length < 8) return "비밀번호는 8자 이상으로 입력해 주세요.";
}

function validateNickname(value: string): string | undefined {
  if (!value) return "닉네임을 입력해 주세요.";
  if (value.length > 20) return "닉네임은 20자 이하로 입력해 주세요.";
}

export default function RegisterPage() {
  const { setMember } = useAuthContext();
  const router = useRouter();

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [errors, setErrors] = useState<FieldErrors>({});
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const [isPending, setIsPending] = useState(false);

  const loginIdError = touched.loginId ? validateLoginId(loginId) : undefined;
  const passwordError = touched.password ? validatePassword(password) : undefined;
  const nicknameError = touched.nickname ? validateNickname(nickname) : undefined;

  const clientErrors = {
    loginId: loginIdError ?? errors.loginId,
    password: passwordError ?? errors.password,
    nickname: nicknameError ?? errors.nickname,
  };

  const isFormValid =
    !validateLoginId(loginId) &&
    !validatePassword(password) &&
    !validateNickname(nickname);

  function handleBlur(field: string) {
    setTouched((prev) => ({ ...prev, [field]: true }));
    // 서버 에러는 다시 타이핑하면 초기화
    setErrors((prev) => ({ ...prev, [field]: undefined }));
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setTouched({ loginId: true, password: true, nickname: true });

    if (!isFormValid) return;

    setIsPending(true);
    try {
      const member = await signupApi({ loginId, password, nickname });
      setMember(member);
      router.push("/lectures");
    } catch (err) {
      if (err instanceof SignupApiError) {
        if (err.fieldErrors) {
          // @Valid 서버 검증 실패 (400) → 필드별 메시지
          setErrors(err.fieldErrors);
          setTouched({ loginId: true, password: true, nickname: true });
        } else if (err.status === 409) {
          // 중복 아이디 (409) → loginId 필드 아래 메시지
          setErrors({ loginId: err.message });
          setTouched((prev) => ({ ...prev, loginId: true }));
        } else {
          alert(err.message);
        }
      } else {
        alert("회원가입에 실패했습니다. 다시 시도해 주세요.");
      }
    } finally {
      setIsPending(false);
    }
  }

  return (
    <div className="flex-1 flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 mb-1">회원가입</h1>
          <p className="text-sm text-gray-500">개발자 성장 여정을 시작하세요.</p>
        </div>

        {/* Form */}
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Input
            label="닉네임"
            id="nickname"
            type="text"
            placeholder="닉네임을 입력하세요"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            onBlur={() => handleBlur("nickname")}
            error={clientErrors.nickname}
          />
          <Input
            label="아이디"
            id="user-id"
            type="text"
            placeholder="영문·숫자 4~20자"
            value={loginId}
            onChange={(e) => setLoginId(e.target.value)}
            onBlur={() => handleBlur("loginId")}
            error={clientErrors.loginId}
          />
          <Input
            label="비밀번호"
            id="password"
            type="password"
            placeholder="영문·숫자 포함 8자 이상"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            onBlur={() => handleBlur("password")}
            error={clientErrors.password}
          />

          <Button
            type="submit"
            fullWidth
            className="mt-2 py-3"
            disabled={!isFormValid || isPending}
          >
            {isPending ? "가입 중..." : "가입하기"}
          </Button>
        </form>

        {/* Footer */}
        <p className="mt-4 text-center text-sm text-gray-500">
          이미 계정이 있으신가요?{" "}
          <Link
            href="/login"
            className="text-violet-600 font-medium hover:underline"
          >
            로그인
          </Link>
        </p>
      </div>
    </div>
  );
}
