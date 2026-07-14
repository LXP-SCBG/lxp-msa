"use client";

import { useEffect, useState } from "react";
import { getReviews, likeReview } from "@/lib/api/reviews";
import type { ReviewListResponse } from "@/types/review";
import LectureReviewForm from "@/components/lectures/LectureReviewForm";

type LectureReviewSectionProps = {
  lectureId: number;
};

export default function LectureReviewSection({
  lectureId,
}: LectureReviewSectionProps) {
  const [reviews, setReviews] = useState<ReviewListResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [likeErrorMessage, setLikeErrorMessage] = useState("");
  const [likingReviewId, setLikingReviewId] = useState<number | null>(null);

  async function fetchReviews() {
    try {
      setErrorMessage("");
      const reviewList = await getReviews(lectureId);
      setReviews(reviewList);
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : "후기 목록을 불러오지 못했습니다."
      );
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    fetchReviews();
  }, [lectureId]);

  function renderStars(rating: number) {
    return (
      <div className="mt-1 flex items-center gap-0.5">
        {[1, 2, 3, 4, 5].map((star) => (
          <span
            key={star}
            className={`text-base leading-none ${
              star <= rating ? "text-orange-400" : "text-gray-300"
            }`}
          >
            ★
          </span>
        ))}
      </div>
    );
  }

  async function handleLikeClick(review: ReviewListResponse) {
    const liked = Boolean(review.likedByMe);

    if (liked) {
      return;
    }

    try {
      setLikeErrorMessage("");
      setLikingReviewId(review.reviewId);

      const result = await likeReview(review.reviewId);

      setReviews((prevReviews) =>
        prevReviews.map((item) =>
          item.reviewId === result.reviewId
            ? {
                ...item,
                likedByMe: result.liked,
                likeCount: result.likeCount,
              }
            : item
        )
      );
    } catch (error) {
      setLikeErrorMessage(
        error instanceof Error
          ? error.message
          : "좋아요 등록에 실패했습니다."
      );
    } finally {
      setLikingReviewId(null);
    }
  }

  return (
    <section className="mt-8 rounded-2xl border border-gray-100 bg-white p-8 shadow-sm">
      <div className="mb-5 flex items-center justify-between">
        <h2 className="text-xl font-bold text-gray-900">후기</h2>
        <span className="text-sm text-gray-400">총 {reviews.length}개</span>
      </div>

      <LectureReviewForm lectureId={lectureId} onCreated={fetchReviews} />

      {likeErrorMessage && (
        <p className="mb-4 text-sm text-red-500">{likeErrorMessage}</p>
      )}

      {isLoading ? (
        <p className="text-sm text-gray-400">후기를 불러오는 중입니다.</p>
      ) : errorMessage ? (
        <p className="text-sm text-red-500">{errorMessage}</p>
      ) : reviews.length > 0 ? (
        <div className="flex flex-col gap-4">
          {reviews.map((review) => {
            const liked = Boolean(review.likedByMe);
            const likeCount = review.likeCount ?? 0;
            const isLiking = likingReviewId === review.reviewId;

            return (
              <article
                key={review.reviewId}
                className="rounded-xl border border-gray-100 bg-white px-5 py-5"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="flex items-start gap-3">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-violet-50 text-sm font-bold text-violet-600">
                    </div>

                    <div>
                      <p className="text-sm font-bold text-gray-900">
                        작성자 ID : {review.writerId}
                      </p>

                      {renderStars(review.rating)}
                    </div>
                  </div>

                  <p className="text-xs text-gray-400">
                    {new Date(review.createdAt).toLocaleDateString("ko-KR")}
                  </p>
                </div>

                <p className="mt-4 text-sm leading-6 text-gray-700">
                  {review.content}
                </p>

                <div className="mt-4">
                  <button
                    type="button"
                    onClick={() => handleLikeClick(review)}
                    disabled={liked || isLiking}
                    className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-sm font-medium transition-colors disabled:cursor-default ${
                      liked
                        ? "border-violet-200 bg-violet-50 text-violet-600"
                        : "border-gray-200 bg-white text-gray-400 hover:border-violet-200 hover:bg-violet-50 hover:text-violet-600"
                    }`}
                    aria-label="후기 좋아요"
                  >
                    <span>{liked ? "♥" : "♡"}</span>
                    <span>좋아요</span>
                    <span>{likeCount}</span>
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center rounded-xl bg-gray-50 py-12">
          <p className="text-sm text-gray-400">작성된 후기가 없습니다.</p>
        </div>
      )}
    </section>
  );
}