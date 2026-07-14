import ReviewCard from "@/components/reviews/ReviewCard";

const reviews = [
  {
    id: 1,
    authorName: "박지원",
    rating: 5,
    date: "2026.06.10",
    content:
      "실무에서 바로 쓸 수 있는 내용이 많아서 좋았어요. 특히 Hooks 파트 설명이 명쾌합니다.",
    likes: 24,
    likedByMe: false,
  },
  {
    id: 2,
    authorName: "김태현",
    rating: 4,
    date: "2026.06.08",
    content:
      "전반적으로 만족스럽지만 후반부 상태관리 파트는 조금 빠르게 지나간 느낌이에요.",
    likes: 9,
    likedByMe: true,
  },
  {
    id: 3,
    authorName: "이서아",
    rating: 5,
    date: "2026.06.05",
    content:
      "입문자도 따라가기 쉽게 차근차근 설명해주셔서 완강했습니다. 강력 추천!",
    likes: 31,
    likedByMe: false,
  },
];

const avgRating = (
  reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length
).toFixed(1);

export default function ReviewTab() {
  return (
    <div>
      {/* Summary Card */}
      <div className="bg-white rounded-xl border border-gray-100 p-5 flex items-center gap-6 mb-4">
        <div className="flex flex-col items-center gap-1 flex-shrink-0">
          <span className="text-3xl font-bold text-gray-900">{avgRating}</span>
          <div className="flex gap-0.5">
            {[1, 2, 3, 4, 5].map((i) => (
              <span key={i} className="text-base text-amber-400">
                ★
              </span>
            ))}
          </div>
        </div>

        <p className="flex-1 text-sm text-gray-500">
          수강생 {reviews.length}명이 남긴 후기
        </p>

        <button className="px-4 py-2.5 bg-violet-600 text-white text-sm font-medium rounded-lg hover:bg-violet-700 transition-colors flex-shrink-0">
          후기 작성
        </button>
      </div>

      {/* Review List */}
      <div className="flex flex-col gap-3">
        {reviews.map((review) => (
          <ReviewCard key={review.id} {...review} />
        ))}
      </div>
    </div>
  );
}
