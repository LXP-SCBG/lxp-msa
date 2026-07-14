package com.ohgiraffers.lectureservice.lecture.dto;

import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import java.util.List;

public record LectureDetailResponse(
	Long lectureId,
	Long instructorId,
	String nickname,
	String title,
	String description,
	List<ContentResponse> contents
) {

	public static LectureDetailResponse of(
		Lecture lecture,
		String nickname,
		List<ContentResponse> contents
	) {
		return new LectureDetailResponse(
			lecture.getId(),
			lecture.getInstructorId(),
			nickname,
			lecture.getTitle(),
			lecture.getDescription(),
			contents
		);
	}
}