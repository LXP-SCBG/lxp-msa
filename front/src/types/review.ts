export type ReviewListResponse = {
  reviewId: number;
  lectureId: number;
  writerId: number;
  content: string;
  rating: number;
  createdAt: string;
  likedByMe: boolean;
  likeCount: number;
};

export type ReviewCreateRequest = {
  content: string;
  rating: number;
};

export type ReviewResponse = {
  reviewId: number;
  lectureId: number;
  writerId: number;
  content: string;
  rating: number;
  createdAt: string;
};

export type ReviewLikeResponse = {
  reviewId: number;
  liked: boolean;
  likeCount: number;
};

export type ErrorResponse = {
  message: string;
  fieldErrors?: Record<string, string>;
};