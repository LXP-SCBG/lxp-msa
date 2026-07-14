"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ApiError } from "@/lib/api/client";
import {
  createGoal,
  deleteGoal,
  getGoals,
  toggleDetailCompletion,
  updateGoal,
} from "@/lib/api/goals";
import type { Goal } from "@/types/goal";

// ─── Types ───────────────────────────────────────────
type View = "list" | "add" | "edit";
// 폼 한 줄. 기존 항목은 detailGoalId 보유, 신규는 null. key는 React 렌더용 안정 키.
type FormItem = { key: string; detailGoalId: number | null; text: string };

// ─── Helpers ──────────────────────────────────────────
const EXPIRE_HOURS = 24;

/** 생성 시각 기준 만료까지 남은 시간 표시 (백엔드엔 마감일이 없고 24h 자동 만료) */
function formatRemaining(createdAt: string): string {
  const created = new Date(createdAt).getTime();
  const remainMs = created + EXPIRE_HOURS * 60 * 60 * 1000 - Date.now();
  if (remainMs <= 0) return "만료됨";
  const hours = Math.floor(remainMs / (60 * 60 * 1000));
  const mins = Math.floor((remainMs % (60 * 60 * 1000)) / (60 * 1000));
  return hours > 0 ? `${hours}시간 후 만료` : `${mins}분 후 만료`;
}

function getTodayStr(): string {
  const t = new Date();
  return `${t.getFullYear()}.${String(t.getMonth() + 1).padStart(2, "0")}.${String(t.getDate()).padStart(2, "0")}`;
}

let nextItemKey = 0;
function newFormItem(): FormItem {
  return { key: `new-${nextItemKey++}`, detailGoalId: null, text: "" };
}

// ─── Icons ────────────────────────────────────────────
function CloseIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function RemoveIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
      <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function PencilIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
      <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
      <path d="M10 11v6M14 11v6M9 6V4h6v2" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

// ─── Component ────────────────────────────────────────
export default function GoalModal({ onClose }: { onClose: () => void }) {
  const router = useRouter();

  const [view, setView] = useState<View>("list");
  const [goals, setGoals] = useState<Goal[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [editingGoalId, setEditingGoalId] = useState<number | null>(null);

  // Form state
  const [formTitle, setFormTitle] = useState("");
  const [formItems, setFormItems] = useState<FormItem[]>([]);

  // ── 공통 에러 처리 ──
  // 비로그인(401)이면 안내 후 로그인 페이지로 이동, 그 외는 메시지 alert.
  const handleError = (e: unknown) => {
    if (e instanceof ApiError && e.status === 401) {
      alert("로그인이 필요한 기능입니다");
      onClose();
      router.push("/login");
      return;
    }
    alert(e instanceof Error ? e.message : "오류가 발생했습니다.");
  };

  const reload = async () => {
    const data = await getGoals();
    setGoals(data);
  };

  // 모달이 열릴 때 오늘의 목표 로드
  useEffect(() => {
    let active = true;
    getGoals()
      .then((data) => {
        if (active) setGoals(data);
      })
      .catch((e) => {
        if (active) handleError(e);
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Actions ──
  const openAdd = () => {
    setFormTitle("");
    setFormItems([newFormItem()]);
    setView("add");
  };

  const openEdit = (goal: Goal) => {
    setEditingGoalId(goal.learningGoalId);
    setFormTitle(goal.title);
    setFormItems(
      goal.detailGoals.map((d) => ({
        key: `d-${d.detailGoalId}`,
        detailGoalId: d.detailGoalId,
        text: d.content,
      })),
    );
    setView("edit");
  };

  const handleDelete = async (goalId: number) => {
    if (!confirm("정말 이 목표를 삭제할까요?")) return;
    try {
      await deleteGoal(goalId);
      await reload();
    } catch (e) {
      handleError(e);
    }
  };

  const handleToggleItem = async (
    goalId: number,
    detailGoalId: number,
    current: boolean,
  ) => {
    try {
      const updated = await toggleDetailCompletion(goalId, detailGoalId, !current);
      setGoals((prev) =>
        prev.map((g) => (g.learningGoalId === updated.learningGoalId ? updated : g)),
      );
    } catch (e) {
      handleError(e);
    }
  };

  const handleSaveAdd = async () => {
    const title = formTitle.trim();
    const contents = formItems.map((i) => i.text.trim()).filter(Boolean);
    if (!title) {
      alert("학습 목표 제목을 입력해 주세요.");
      return;
    }
    if (contents.length === 0) {
      alert("세부 목표를 최소 1개 이상 입력해 주세요.");
      return;
    }
    try {
      setSubmitting(true);
      await createGoal({ title, detailGoals: contents.map((content) => ({ content })) });
      await reload();
      setView("list");
    } catch (e) {
      handleError(e);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSaveEdit = async () => {
    if (editingGoalId === null) return;
    const title = formTitle.trim();
    const items = formItems.filter((i) => i.text.trim());
    if (!title) {
      alert("학습 목표 제목을 입력해 주세요.");
      return;
    }
    if (items.length === 0) {
      alert("세부 목표는 최소 1개 이상 유지해야 합니다.");
      return;
    }
    try {
      setSubmitting(true);
      await updateGoal(editingGoalId, {
        title,
        detailGoals: items.map((i) => ({
          detailGoalId: i.detailGoalId,
          content: i.text.trim(),
        })),
      });
      await reload();
      setView("list");
    } catch (e) {
      handleError(e);
    } finally {
      setSubmitting(false);
    }
  };

  const addFormItem = () => setFormItems((prev) => [...prev, newFormItem()]);

  const removeFormItem = (key: string) =>
    setFormItems((prev) => prev.filter((i) => i.key !== key));

  const updateFormItemText = (key: string, text: string) =>
    setFormItems((prev) => prev.map((i) => (i.key === key ? { ...i, text } : i)));

  const isFormView = view === "add" || view === "edit";

  // ── Render ──
  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-lg mx-4 max-h-[85vh] flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 pt-6 pb-4 flex-shrink-0">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                {view === "list" ? "학습 목표" : view === "add" ? "새 목표 추가" : "목표 수정"}
              </h2>
              {view === "list" && (
                <p className="text-xs text-gray-400 mt-0.5">오늘 · {getTodayStr()}</p>
              )}
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600 transition-colors mt-0.5"
            >
              <CloseIcon />
            </button>
          </div>
        </div>

        {/* Scrollable Body */}
        <div className="flex-1 overflow-y-auto px-6 min-h-0">
          {/* ── LIST VIEW ── */}
          {view === "list" && (
            <div className="flex flex-col gap-4 pb-4">
              {loading && (
                <p className="text-center text-sm text-gray-400 py-10">불러오는 중…</p>
              )}
              {!loading && goals.length === 0 && (
                <p className="text-center text-sm text-gray-400 py-10">
                  오늘 목표를 설정하지 않았습니다.
                </p>
              )}
              {!loading &&
                goals.map((goal) => {
                  const doneCount = goal.detailGoals.filter((d) => d.completed).length;
                  return (
                    <div
                      key={goal.learningGoalId}
                      className={`rounded-xl p-4 transition-colors ${
                        goal.completed
                          ? "bg-green-50 ring-1 ring-green-200"
                          : "bg-gray-50"
                      }`}
                    >
                      {/* Goal header */}
                      <div className="flex items-start justify-between mb-1">
                        <h3
                          className={`font-semibold text-sm leading-snug pr-2 ${
                            goal.completed ? "text-green-700" : "text-gray-900"
                          }`}
                        >
                          {goal.completed && "✓ "}
                          {goal.title}
                        </h3>
                        <div className="flex items-center gap-2 flex-shrink-0">
                          <button
                            onClick={() => openEdit(goal)}
                            className="text-gray-400 hover:text-violet-600 transition-colors"
                          >
                            <PencilIcon />
                          </button>
                          <button
                            onClick={() => handleDelete(goal.learningGoalId)}
                            className="text-gray-400 hover:text-red-500 transition-colors"
                          >
                            <TrashIcon />
                          </button>
                        </div>
                      </div>
                      {/* Progress */}
                      <p className="text-xs text-gray-400 mb-3">
                        {doneCount}/{goal.detailGoals.length} 완료 · {formatRemaining(goal.createdAt)}
                      </p>
                      {/* Sub-items */}
                      <div className="flex flex-col gap-2.5">
                        {goal.detailGoals.map((item) => (
                          <button
                            key={item.detailGoalId}
                            onClick={() =>
                              handleToggleItem(goal.learningGoalId, item.detailGoalId, item.completed)
                            }
                            className="flex items-center gap-2.5 text-left w-full"
                          >
                            <span
                              className={`w-5 h-5 rounded flex items-center justify-center flex-shrink-0 transition-colors border-2 ${
                                item.completed
                                  ? "bg-violet-600 border-violet-600"
                                  : "bg-white border-gray-300"
                              }`}
                            >
                              {item.completed && <CheckIcon />}
                            </span>
                            <span
                              className={`text-sm transition-colors ${
                                item.completed ? "text-gray-400 line-through" : "text-gray-700"
                              }`}
                            >
                              {item.content}
                            </span>
                          </button>
                        ))}
                      </div>
                    </div>
                  );
                })}
            </div>
          )}

          {/* ── ADD / EDIT FORM ── */}
          {isFormView && (
            <div className="flex flex-col gap-5 pb-4">
              {/* Title */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-sm font-medium text-gray-700">학습 목표 제목</label>
                  <span className="text-xs text-gray-400">{formTitle.length}/30</span>
                </div>
                <input
                  type="text"
                  maxLength={30}
                  placeholder="예: React 핵심 개념 복습"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  className="w-full px-3 py-2.5 text-sm border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent transition"
                />
              </div>

              {/* Sub-items */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">세부 목표</label>
                <div className="flex flex-col gap-2 mb-2">
                  {formItems.map((item) => (
                    <div key={item.key} className="flex items-center gap-2">
                      <span className="text-violet-400 text-xl leading-none flex-shrink-0">·</span>
                      <input
                        type="text"
                        maxLength={50}
                        placeholder="세부 목표를 입력하세요"
                        value={item.text}
                        onChange={(e) => updateFormItemText(item.key, e.target.value)}
                        className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent transition"
                      />
                      <button
                        onClick={() => removeFormItem(item.key)}
                        className="text-gray-400 hover:text-gray-600 flex-shrink-0 p-0.5"
                      >
                        <RemoveIcon />
                      </button>
                    </div>
                  ))}
                </div>
                <button
                  onClick={addFormItem}
                  disabled={formItems.length >= 20}
                  className="w-full py-2.5 border border-dashed border-violet-300 text-violet-600 text-sm rounded-lg hover:bg-violet-50 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  + 항목 추가
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 pb-6 pt-3 flex-shrink-0">
          {view === "list" && (
            <button
              onClick={openAdd}
              className="w-full py-3 bg-violet-50 text-violet-600 text-sm font-medium rounded-xl hover:bg-violet-100 transition-colors"
            >
              + 새 목표 추가
            </button>
          )}
          {isFormView && (
            <div className="flex gap-3">
              <button
                onClick={() => setView("list")}
                className="flex-1 py-2.5 border border-gray-200 text-gray-600 text-sm font-medium rounded-xl hover:bg-gray-50 transition-colors"
              >
                취소
              </button>
              <button
                onClick={view === "add" ? handleSaveAdd : handleSaveEdit}
                disabled={submitting || !formTitle.trim()}
                className="flex-1 py-2.5 bg-violet-600 text-white text-sm font-medium rounded-xl hover:bg-violet-700 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {view === "add" ? "목표 등록" : "수정하기"}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
