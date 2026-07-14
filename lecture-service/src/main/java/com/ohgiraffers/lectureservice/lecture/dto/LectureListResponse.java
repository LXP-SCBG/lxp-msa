package com.ohgiraffers.lectureservice.lecture.dto;


import com.ohgiraffers.lectureservice.lecture.domain.Lecture;

public record LectureListResponse(
	Long lectureId,
	Long instructorId,
	String nickname,
	String title,
	String description
) {
	public static LectureListResponse of(
		Lecture lecture,
		String nickname
	) {
		return new LectureListResponse(
			lecture.getId(),
			lecture.getInstructorId(),
			nickname,
			lecture.getTitle(),
			lecture.getDescription()
		);
	}
}