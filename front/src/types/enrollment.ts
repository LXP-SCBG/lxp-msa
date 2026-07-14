export type EnrollRequest = {
  lectureId: number;
};

export type EnrollmentResponse = {
  enrollmentId: number;
  lectureId: number;
  lectureTitle: string | null;
  enrolledAt: string;
};