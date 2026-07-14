import Link from "next/link";

type LectureCardProps = {
  id: number;
  title: string;
  instructor: string;
  level: string;
  rating: number;
  students: string;
  lessons: number;
  category: string;
  enrolled: boolean;
  watermark: string;
};

export default function LectureCard({
  id,
  title,
  instructor,
  level,
  rating,
  students,
  lessons,
  category,
  enrolled,
  watermark,
}: LectureCardProps) {
  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex flex-col">
      {/* Thumbnail */}
      <div className="relative h-36 bg-violet-100 overflow-hidden flex-shrink-0">
        <span className="absolute top-3 left-3 z-10 bg-white text-violet-600 text-xs font-medium px-3 py-1 rounded-full">
          {category}
        </span>
        <span
          className="absolute -right-4 bottom-0 text-8xl font-extrabold text-violet-200 leading-none select-none"
          aria-hidden="true"
        >
          {watermark}
        </span>
      </div>

      {/* Content */}
      <div className="p-5 flex flex-col gap-2 flex-1">
        <h3 className="font-semibold text-gray-900 text-base leading-snug">
          {title}
        </h3>
        <p className="text-sm text-gray-500">
          {instructor} · {level}
        </p>
        <div className="flex items-center gap-1.5 text-sm text-gray-600">
          <span className="text-amber-400 text-base">★</span>
          <span className="font-medium">{rating}</span>
          <span className="text-gray-300">·</span>
          <span>수강생 {students}</span>
          <span className="text-gray-300">·</span>
          <span>{lessons}차시</span>
        </div>

        {/* CTA Button */}
        <div className="mt-auto pt-2">
          {enrolled ? (
            <Link
              href={`/lectures/${id}`}
              className="block w-full py-2.5 rounded-xl bg-violet-100 text-violet-700 text-sm font-medium hover:bg-violet-200 transition-colors text-center"
            >
              ✓ 수강 중 · 이어보기
            </Link>
          ) : (
            <Link
              href={`/lectures/${id}`}
              className="block w-full py-2.5 rounded-xl border border-violet-600 text-violet-600 text-sm font-medium bg-white hover:bg-violet-50 transition-colors text-center"
            >
              자세히 보기 &gt;
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
