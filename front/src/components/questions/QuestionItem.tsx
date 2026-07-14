import type { QuestionListResponse } from "@/types/question";

type QuestionItemProps = {
  question: QuestionListResponse;
  onClick?: () => void;
};

export default function QuestionItem({ question, onClick }: QuestionItemProps) {
  return (
    <div
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      onClick={onClick}
      onKeyDown={onClick ? (e) => e.key === "Enter" && onClick() : undefined}
      className={`rounded-2xl border border-gray-100 bg-white px-5 py-4 shadow-sm transition-all ${
        onClick
          ? "cursor-pointer hover:border-violet-200 hover:shadow-md"
          : "cursor-default"
      }`}
    >
      {/* Title */}
      <h4 className="text-[15px] font-semibold leading-snug text-gray-900">
        {question.title}
      </h4>

      {/* Meta */}
      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-gray-400">
        <span>{question.writerNickname}</span>
        <span>·</span>
        <span>{new Date(question.createdAt).toLocaleDateString("ko-KR")}</span>
      </div>
    </div>
  );
}