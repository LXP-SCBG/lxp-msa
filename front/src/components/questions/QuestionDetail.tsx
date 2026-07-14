"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import {
  getQuestion,
  updateQuestion,
  deleteQuestion,
} from "@/lib/api/questions";
import { useAuthContext } from "@/store/authStore";
import type { QuestionDetailResponse } from "@/types/question";

type QuestionDetailProps = {
  questionId: number;
  onBack: () => void;
  onDeleted?: () => void;
};

export default function QuestionDetail({
  questionId,
  onBack,
  onDeleted,
}: QuestionDetailProps) {
  const router = useRouter();
  const { member } = useAuthContext();
  const [question, setQuestion] = useState<QuestionDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // 편집 상태
  const [editing, setEditing] = useState(false);
  const [editTitle, setEditTitle] = useState("");
  const [editContent, setEditContent] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 본인 작성 질문 여부 (수정/삭제 버튼 노출 기준)
  const isMine =
    member != null && question != null && member.memberId === question.writerId;

  // 학습목표와 동일한 에러 처리: 401이면 로그인 페이지, 그 외는 백엔드 메시지 alert
  const handleError = (e: unknown) => {
    if (e instanceof ApiError && e.status === 401) {
      alert("로그인이 필요한 기능입니다");
      router.push("/login");
      return;
    }
    alert(e instanceof Error ? e.message : "오류가 발생했습니다.");
  };

  const loadQuestion = () => {
    setLoading(true);
    setError("");
    getQuestion(questionId)
      .then(setQuestion)
      .catch((e) =>
        setError(e instanceof Error ? e.message : "질문을 불러오지 못했습니다."),
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    getQuestion(questionId)
      .then((data) => {
        if (active) setQuestion(data);
      })
      .catch((e) => {
        if (active) {
          setError(e instanceof Error ? e.message : "질문을 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [questionId]);

  const openEdit = () => {
    if (!question) return;
    setEditTitle(question.title);
    setEditContent(question.content);
    setEditing(true);
  };

  const handleUpdate = async () => {
    const title = editTitle.trim();
    const content = editContent.trim();
    if (!title || !content) {
      alert("제목과 내용을 모두 입력해 주세요.");
      return;
    }
    try {
      setSubmitting(true);
      await updateQuestion(questionId, { title, content });
      setEditing(false);
      loadQuestion(); // 수정 후 최신 내용 다시 조회
    } catch (e) {
      handleError(e);
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm("정말 이 질문을 삭제할까요?")) return;
    try {
      await deleteQuestion(questionId);
      onDeleted?.();
    } catch (e) {
      handleError(e);
    }
  };

  return (
    <div>
      {/* Back */}
      <button
        onClick={onBack}
        className="mb-5 inline-flex items-center gap-1 text-sm text-gray-500 transition-colors hover:text-gray-700"
      >
        <span>‹</span>
        <span>질문 목록</span>
      </button>

      {loading ? (
        <p className="py-16 text-center text-sm text-gray-400">불러오는 중...</p>
      ) : error ? (
        <div className="flex items-center justify-center rounded-xl border border-gray-100 bg-white py-16 text-sm text-red-500">
          {error}
        </div>
      ) : question ? (
        <div className="rounded-xl border border-gray-100 bg-white p-6">
          {editing ? (
            /* ── 편집 뷰 ── */
            <div className="flex flex-col gap-4">
              <h2 className="text-lg font-bold text-gray-900">질문 수정</h2>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-gray-700">
                  제목
                </label>
                <input
                  type="text"
                  maxLength={255}
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full rounded-lg border border-gray-200 px-3 py-2.5 text-sm outline-none transition focus:border-transparent focus:ring-2 focus:ring-violet-500"
                />
              </div>
              <div>
                <label className="mb-1.5 block text-sm font-medium text-gray-700">
                  내용
                </label>
                <textarea
                  rows={6}
                  value={editContent}
                  onChange={(e) => setEditContent(e.target.value)}
                  className="w-full resize-none rounded-lg border border-gray-200 px-3 py-2.5 text-sm outline-none transition focus:border-transparent focus:ring-2 focus:ring-violet-500"
                />
              </div>
              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setEditing(false)}
                  className="flex-1 rounded-xl border border-gray-200 py-2.5 text-sm font-medium text-gray-600 transition-colors hover:bg-gray-50"
                >
                  취소
                </button>
                <button
                  type="button"
                  onClick={handleUpdate}
                  disabled={submitting || !editTitle.trim() || !editContent.trim()}
                  className="flex-1 rounded-xl bg-violet-600 py-2.5 text-sm font-medium text-white transition-colors hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-40"
                >
                  {submitting ? "수정 중..." : "수정하기"}
                </button>
              </div>
            </div>
          ) : (
            /* ── 보기 뷰 ── */
            <>
              {/* Actions (본인만) */}
              {isMine && (
                <div className="mb-3 flex items-center justify-end gap-2">
                  <button
                    type="button"
                    onClick={openEdit}
                    className="rounded-md border border-gray-200 px-3 py-1 text-xs font-medium text-gray-600 transition-colors hover:bg-gray-50"
                  >
                    수정
                  </button>
                  <button
                    type="button"
                    onClick={handleDelete}
                    className="rounded-md border border-red-200 px-3 py-1 text-xs font-medium text-red-500 transition-colors hover:bg-red-50"
                  >
                    삭제
                  </button>
                </div>
              )}

              {/* Title */}
              <h2 className="mb-2 text-lg font-bold text-gray-900">
                {question.title}
              </h2>

              {/* Meta */}
              <div className="mb-5 flex items-center gap-2 text-xs text-gray-400">
                <span>{question.writerNickname}</span>
                <span>·</span>
                <span>
                  {new Date(question.createdAt).toLocaleDateString("ko-KR")}
                </span>
              </div>

              {/* Content */}
              <p className="whitespace-pre-wrap text-sm leading-relaxed text-gray-700">
                {question.content}
              </p>
            </>
          )}
        </div>
      ) : null}
    </div>
  );
}