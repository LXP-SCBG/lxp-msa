"use client";

import { useState, type FormEvent } from "react";
import { createReview } from "@/lib/api/reviews";

type LectureReviewFormProps = {
  lectureId: number;
  onCreated: () => void;
};

export default function LectureReviewForm({
  lectureId,
  onCreated,
}: LectureReviewFormProps) {
  const [content, setContent] = useState("");
  const [rating, setRating] = useState(5);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isDisabled = !content.trim() || isSubmitting;

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    setErrorMessage("");
    setIsSubmitting(true);

    try {
      await createReview(lectureId, {
        content: content.trim(),
        rating,
      });

      setContent("");
      setRating(5);
      onCreated();
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : "후기 등록에 실패했습니다."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="mb-6 rounded-xl border border-gray-100 bg-gray-50 p-4"
    >
      <div className="mb-3 flex items-center justify-between gap-4">
        <h3 className="text-sm font-bold text-gray-900">후기 작성</h3>

        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((star) => (
            <button
              key={star}
              type="button"
              onClick={() => setRating(star)}
              className={`text-2xl leading-none transition-colors ${
                star <= rating ? "text-orange-400" : "text-gray-300"
              }`}
              aria-label={`${star}점`}
            >
              ★
            </button>
          ))}

          <span className="ml-2 text-sm font-medium text-gray-600">
            {rating}점
          </span>
        </div>
      </div>

      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        maxLength={49}
        placeholder="후기를 입력해 주세요. 50자 미만으로 작성할 수 있습니다."
        className="min-h-24 w-full resize-none rounded-lg border border-gray-200 bg-white px-3 py-2 text-sm text-gray-900 outline-none placeholder:text-gray-400 focus:ring-2 focus:ring-violet-500"
      />

      <div className="mt-2 flex items-center justify-between">
        <span className="text-xs text-gray-400">{content.length}/49</span>

        <button
          type="submit"
          disabled={isDisabled}
          className="rounded-lg bg-violet-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-violet-700 disabled:bg-violet-300 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "등록 중..." : "후기 등록"}
        </button>
      </div>

      {errorMessage && (
        <p className="mt-3 text-sm text-red-500">{errorMessage}</p>
      )}
    </form>
  );
}