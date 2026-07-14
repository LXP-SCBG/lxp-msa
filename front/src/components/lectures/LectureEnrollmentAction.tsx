"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import type { LectureDetailResponse } from "@/types/lecture";
import {
  enrollLecture,
  getEnrollments,
  isApiError,
} from "@/lib/api/enrollments";
import { useAuthContext } from "@/store/authStore";

type NoticeType = "success" | "error" | "info";

type Notice = {
  type: NoticeType;
  message: string;
};

type LectureEnrollmentActionProps = {
  lecture: LectureDetailResponse;
};

const BENEFITS = [
  "전체 차시 영상·자료 수강",
  "강사에게 Q&A 질문 등록",
  "학습 목표 설정 및 진행률 추적",
  "수료 후 후기 작성 가능",
];

export default function LectureEnrollmentAction({
  lecture,
}: LectureEnrollmentActionProps) {
  const router = useRouter();
  const { member, loading } = useAuthContext();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCheckingEnrollment, setIsCheckingEnrollment] = useState(true);
  const [isEnrolled, setIsEnrolled] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);

  useEffect(() => {
    if (loading) {
      return;
    }

    if (!member) {
      setIsCheckingEnrollment(false);
      setIsEnrolled(false);
      return;
    }

    let active = true;

    setIsCheckingEnrollment(true);

    getEnrollments()
      .then((enrollments) => {
        if (!active) return;

        const enrolled = enrollments.some(
          (enrollment) => enrollment.lectureId === lecture.lectureId
        );

        setIsEnrolled(enrolled);
      })
      .catch((error) => {
        if (!active) return;

        // 세션 만료/비로그인 상태면 수강 여부만 false 처리한다.
        if (isApiError(error) && error.status === 401) {
          setIsEnrolled(false);
          return;
        }

        // 수강 여부 확인 실패가 상세 페이지 전체 에러로 번지지 않게 막는다.
        setIsEnrolled(false);
      })
      .finally(() => {
        if (active) {
          setIsCheckingEnrollment(false);
        }
      });

    return () => {
      active = false;
    };
  }, [lecture.lectureId, loading, member]);

  const handleMainButtonClick = () => {
    if (loading || isCheckingEnrollment || isSubmitting) {
      return;
    }

    if (!member) {
      router.push("/login");
      return;
    }

    if (isEnrolled) {
    setNotice({
        type: "info",
        message: "준비중입니다.",
    });
    return;
}

    setNotice(null);
    setIsModalOpen(true);
  };

  const handleEnroll = async () => {
    setIsSubmitting(true);
    setNotice(null);

    try {
      await enrollLecture(lecture.lectureId);

      setIsEnrolled(true);
      setIsModalOpen(false);
      setNotice({
        type: "success",
        message: "수강신청이 완료되었습니다!",
      });
    } catch (error) {
      if (isApiError(error) && error.status === 401) {
        setIsModalOpen(false);
        router.push("/login");
        return;
      }

      if (isApiError(error) && error.status === 409) {
        setIsEnrolled(true);
        setIsModalOpen(false);
        setNotice({
          type: "info",
          message: "이미 수강 중인 강의입니다.",
        });
        return;
      }

      setIsModalOpen(false);
      setNotice({
        type: "error",
        message:
          error instanceof Error
            ? error.message
            : "수강 신청에 실패했습니다.",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const noticeClassName = {
    success: "border-emerald-100 bg-emerald-50 text-emerald-700",
    error: "border-red-100 bg-red-50 text-red-600",
    info: "border-violet-100 bg-violet-50 text-violet-700",
  }[notice?.type ?? "success"];

  return (
    <div className="mt-8">
      <button
        type="button"
        onClick={handleMainButtonClick}
        disabled={loading || isCheckingEnrollment || isSubmitting}
        className="inline-flex w-full items-center justify-center rounded-xl bg-violet-600 px-5 py-3 text-sm font-semibold text-white transition-colors hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto"
      >
        {isCheckingEnrollment
          ? "수강 여부 확인 중..."
          : isEnrolled
            ? "이어서 학습하기"
            : "수강 신청하기"}
      </button>

      {notice && (
        <div
          className={`mt-4 rounded-xl border px-4 py-3 text-sm font-medium ${noticeClassName}`}
        >
          {notice.message}
        </div>
      )}

      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <div className="mb-5">
              <h2 className="text-xl font-bold text-gray-900">수강 신청</h2>
              <p className="mt-1 text-sm text-gray-500">
                아래 강의를 수강 신청하시겠습니까?
              </p>
            </div>

            <div className="rounded-xl border border-gray-100 bg-gray-50 p-4">
              <dl className="space-y-3 text-sm">
                <div className="flex justify-between gap-4">
                  <dt className="text-gray-500">강의명</dt>
                  <dd className="text-right font-medium text-gray-900">
                    {lecture.title}
                  </dd>
                </div>

                <div className="flex justify-between gap-4">
                  <dt className="text-gray-500">강사명</dt>
                  <dd className="font-medium text-gray-900">
                    {lecture.nickname}
                  </dd>
                </div>

                <div className="flex justify-between gap-4">
                  <dt className="text-gray-500">난이도</dt>
                  <dd className="font-medium text-gray-900">중급</dd>
                </div>

                <div className="flex justify-between gap-4">
                  <dt className="text-gray-500">수강료</dt>
                  <dd className="font-medium text-violet-700">무료</dd>
                </div>
              </dl>
            </div>

            <div className="mt-5">
              <p className="text-sm font-semibold text-gray-900">
                수강 신청 시 제공 혜택
              </p>

              <ul className="mt-3 space-y-2 text-sm text-gray-600">
                {BENEFITS.map((benefit) => (
                  <li key={benefit} className="flex gap-2">
                    <span className="text-violet-600">✓</span>
                    <span>{benefit}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="mt-6 flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                disabled={isSubmitting}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-60"
              >
                취소
              </button>

              <button
                type="button"
                onClick={handleEnroll}
                disabled={isSubmitting}
                className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-semibold text-white hover:bg-violet-700 disabled:opacity-60"
              >
                {isSubmitting ? "신청 중..." : "수강 신청하기"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}