"use client";

import type { ContentResponse } from "@/types/lecture";

type LectureContentItemProps = {
  content: ContentResponse;
};

export default function LectureContentItem({
  content,
}: LectureContentItemProps) {
  const handleClick = () => {
    alert("콘텐츠 재생/다운로드 기능은 준비 중입니다.");
  };

  return (
    <button
      type="button"
      onClick={handleClick}
      className="flex w-full items-center justify-between rounded-xl border border-gray-100 px-4 py-3 text-left hover:bg-gray-50 transition-colors"
    >
      <div>
        <p className="text-sm font-medium text-gray-900">
          {content.sortOrder}. {content.title}
        </p>
        <p className="mt-1 text-xs text-gray-400">{content.contentUrl}</p>
      </div>

      <span className="text-sm text-gray-400">바로가기</span>
    </button>
  );
}