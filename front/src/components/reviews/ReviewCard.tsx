"use client";

import { useState } from "react";

type ReviewCardProps = {
  authorName: string;
  rating: number;
  date: string;
  content: string;
  likes: number;
  likedByMe: boolean;
};

function Stars({ rating }: { rating: number }) {
  return (
    <div className="flex gap-0.5">
      {[1, 2, 3, 4, 5].map((i) => (
        <span
          key={i}
          className={`text-sm ${i <= rating ? "text-amber-400" : "text-gray-200"}`}
        >
          ★
        </span>
      ))}
    </div>
  );
}

export default function ReviewCard({
  authorName,
  rating,
  date,
  content,
  likes,
  likedByMe,
}: ReviewCardProps) {
  const [liked, setLiked] = useState(likedByMe);
  const [likeCount, setLikeCount] = useState(likes);

  const toggleLike = () => {
    setLiked((prev) => !prev);
    setLikeCount((prev) => (liked ? prev - 1 : prev + 1));
  };

  return (
    <div className="bg-white rounded-xl border border-gray-100 p-5">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-full bg-violet-100 flex items-center justify-center text-sm font-bold text-violet-600 flex-shrink-0">
            {authorName.charAt(0)}
          </div>
          <div>
            <p className="text-sm font-semibold text-gray-800">{authorName}</p>
            <Stars rating={rating} />
          </div>
        </div>
        <span className="text-xs text-gray-400">{date}</span>
      </div>

      {/* Content */}
      <p className="text-sm text-gray-700 leading-relaxed mb-4">{content}</p>

      {/* Like button */}
      <button
        onClick={toggleLike}
        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
          liked
            ? "bg-violet-600 text-white"
            : "border border-gray-200 text-gray-500 hover:border-gray-300 hover:text-gray-700"
        }`}
      >
        {liked ? "♥" : "♡"} 좋아요 {likeCount}
      </button>
    </div>
  );
}
