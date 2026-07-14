"use client";

export type LectureTabType = "contents" | "questions" | "reviews";

type LectureTabMenuProps = {
  activeTab: LectureTabType;
  onTabChange: (tab: LectureTabType) => void;
};

export default function LectureTabMenu({
  activeTab,
  onTabChange,
}: LectureTabMenuProps) {
  const getButtonClassName = (tab: LectureTabType) => {
    const base = "px-5 py-3 text-sm font-semibold transition-colors";

    if (activeTab === tab) {
      return `${base} border-b-2 border-violet-600 text-violet-600`;
    }

    return `${base} text-gray-400 hover:text-gray-600`;
  };

  return (
    <div className="mt-8 flex items-center gap-2 border-b border-gray-200">
      <button
        type="button"
        onClick={() => onTabChange("contents")}
        className={getButtonClassName("contents")}
      >
        콘텐츠
      </button>

      <button
        type="button"
        onClick={() => onTabChange("questions")}
        className={getButtonClassName("questions")}
      >
        Q&amp;A
      </button>

      <button
        type="button"
        onClick={() => onTabChange("reviews")}
        className={getButtonClassName("reviews")}
      >
        후기
      </button>
    </div>
  );
}