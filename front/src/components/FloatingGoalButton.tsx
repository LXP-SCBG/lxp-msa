"use client";

import { useState } from "react";
import GoalModal from "@/components/goals/GoalModal";

function TargetIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="10" />
      <circle cx="12" cy="12" r="6" />
      <circle cx="12" cy="12" r="2" />
    </svg>
  );
}

export default function FloatingGoalButton() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      {/* 버튼 컨테이너 — ping 애니메이션의 기준점 */}
      <div className="fixed bottom-6 right-6 z-50">
        {/* 배경 파동 — 목표가 있음을 암시하는 펄스 링 */}
        <span
          className="absolute inset-0 rounded-full bg-violet-400 animate-ping"
          style={{ opacity: 0.25 }}
        />
        {/* 실제 버튼 */}
        <button
          onClick={() => setIsOpen(true)}
          className="relative flex items-center gap-2 bg-violet-600 hover:bg-violet-700 active:scale-95 text-white text-sm font-medium px-4 py-3 rounded-full shadow-lg transition-all duration-150"
        >
          <TargetIcon />
          학습 목표
        </button>
      </div>

      {isOpen && <GoalModal onClose={() => setIsOpen(false)} />}
    </>
  );
}
