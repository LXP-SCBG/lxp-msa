"use client";

import { useState } from "react";
import type { LectureDetailResponse } from "@/types/lecture";
import LectureContentItem from "@/components/lectures/LectureContentItem";
import LectureReviewSection from "@/components/lectures/LectureReviewSection";
import LectureTabMenu, {
  type LectureTabType,
} from "@/components/lectures/LectureTabMenu";
import QnaTab from "@/components/questions/QnaTab";

type LectureDetailTabsProps = {
  lecture: LectureDetailResponse;
};

export default function LectureDetailTabs({ lecture }: LectureDetailTabsProps) {
  const [activeTab, setActiveTab] = useState<LectureTabType>("contents");

  return (
    <>
      <LectureTabMenu activeTab={activeTab} onTabChange={setActiveTab} />

      {activeTab === "contents" && (
        <section className="mt-8 rounded-2xl border border-gray-100 bg-white p-8 shadow-sm">
          <div className="mb-5 flex items-center justify-between">
            <h2 className="text-xl font-bold text-gray-900">콘텐츠 목록</h2>
            <span className="text-sm text-gray-400">
              총 {lecture.contents.length}개
            </span>
          </div>

          {lecture.contents.length > 0 ? (
            <div className="flex flex-col gap-3">
              {lecture.contents.map((content) => (
                <LectureContentItem key={content.contentId} content={content} />
              ))}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center rounded-xl bg-gray-50 py-12">
              <p className="text-sm text-gray-400">
                등록된 콘텐츠가 없습니다.
              </p>
            </div>
          )}
        </section>
      )}

      {activeTab === "questions" && (
        <section className="mt-8 rounded-2xl border border-gray-100 bg-white p-8 shadow-sm">
          <QnaTab lectureId={lecture.lectureId} />
        </section>
      )}

      {activeTab === "reviews" && (
        <LectureReviewSection lectureId={lecture.lectureId} />
      )}
    </>
  );
}