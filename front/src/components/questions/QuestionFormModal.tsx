"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import { createQuestion } from "@/lib/api/questions";

type QuestionFormModalProps = {
  lectureId: number;
  onClose: () => void;
  onCreated: () => void;
};

export default function QuestionFormModal({
  lectureId,
  onClose,
  onCreated,
}: QuestionFormModalProps) {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const handleError = (e: unknown) => {
    if (e instanceof ApiError && e.status === 401) {
      alert("로그인이 필요한 기능입니다");
      onClose();
      router.push("/login");
      return;
    }
    alert(e instanceof Error ? e.message : "오류가 발생했습니다.");
  };

  const handleSubmit = async () => {
    if (!title.trim() || !content.trim()) {
      alert("제목과 내용을 모두 입력해 주세요.");
      return;
    }
    try {
      setSubmitting(true);
      await createQuestion({
        lectureId,
        title: title.trim(),
        content: content.trim(),
        isPublic: true, // MVP: 공개 고정 (비공개 선택은 추후 추가)
      });
      onCreated();
    } catch (e) {
      handleError(e);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      <div
        className="mx-4 flex max-h-[85vh] w-full max-w-lg flex-col rounded-2xl bg-white shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex-shrink-0 px-6 pb-4 pt-6">
          <h2 className="text-xl font-bold text-gray-900">질문 등록</h2>
        </div>

        {/* Body */}
        <div className="min-h-0 flex-1 overflow-y-auto px-6">
          <div className="flex flex-col gap-5 pb-4">
            {/* 제목 */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">
                제목
              </label>
              <input
                type="text"
                maxLength={255}
                placeholder="질문 제목을 입력하세요"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm outline-none transition focus:border-transparent focus:ring-2 focus:ring-violet-500"
              />
            </div>

            {/* 내용 */}
            <div>
              <label className="mb-1.5 block text-sm font-medium text-gray-700">
                내용
              </label>
              <textarea
                rows={6}
                placeholder="궁금한 점을 자세히 적어주세요"
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full resize-none rounded-lg border border-gray-200 px-3 py-2.5 text-sm outline-none transition focus:border-transparent focus:ring-2 focus:ring-violet-500"
              />
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="flex flex-shrink-0 gap-3 px-6 pb-6 pt-3">
          <button
            type="button"
            onClick={onClose}
            className="flex-1 rounded-xl border border-gray-200 py-2.5 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50"
          >
            취소
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            disabled={submitting || !title.trim() || !content.trim()}
            className="flex-1 rounded-xl bg-violet-600 py-2.5 text-sm font-medium text-white transition-colors hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {submitting ? "등록 중..." : "질문 등록"}
          </button>
        </div>
      </div>
    </div>
  );
}