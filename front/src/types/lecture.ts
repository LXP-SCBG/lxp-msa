export type LectureListResponse = {
  lectureId: number;
  instructorId: number;
  nickname: string;
  title: string;
  description: string | null;
};

export type ContentResponse = {
  contentId: number;
  title: string;
  contentUrl: string;
  sortOrder: number;
};

export type LectureDetailResponse = {
  lectureId: number;
  instructorId: number;
  nickname: string;
  title: string;
  description: string | null;
  contents: ContentResponse[];
};