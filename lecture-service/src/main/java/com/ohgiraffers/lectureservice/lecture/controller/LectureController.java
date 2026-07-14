package com.ohgiraffers.lectureservice.lecture.controller;

import com.ohgiraffers.lectureservice.common.exception.BusinessException;
import com.ohgiraffers.lectureservice.common.exception.ErrorCode;
import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import com.ohgiraffers.lectureservice.lecture.domain.LectureStatus;
import com.ohgiraffers.lectureservice.lecture.dto.LectureDetailResponse;
import com.ohgiraffers.lectureservice.lecture.dto.LectureListResponse;
import com.ohgiraffers.lectureservice.lecture.dto.LectureResponse;
import com.ohgiraffers.lectureservice.lecture.repository.LectureRepository;
import com.ohgiraffers.lectureservice.lecture.service.LectureService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/lectures")
public class LectureController {

	private final LectureService lectureService;
	private final LectureRepository lectureRepository;

	public LectureController(LectureService lectureService, LectureRepository lectureRepository) {
		this.lectureService = lectureService;
		this.lectureRepository = lectureRepository;
	}

	@GetMapping
	public ResponseEntity<List<LectureListResponse>> findAllLectures() {
		return ResponseEntity.ok(lectureService.findAllLectures());
	}

	@GetMapping("/{lectureId}")
	public ResponseEntity<LectureDetailResponse> findLecture(
			@PathVariable Long lectureId
	) {
		return ResponseEntity.ok(lectureService.findLecture(lectureId));
	}

	@GetMapping("/api/v1/lectures/{id}")
	public ResponseEntity<LectureResponse> findLectureById(@PathVariable Long id) {
		Lecture lecture = lectureRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

		if (lecture.getStatus() != LectureStatus.PUBLIC) {
			throw new BusinessException(ErrorCode.LECTURE_NOT_ACCESSIBLE);
		}
		return ResponseEntity.ok(LectureResponse.from(lecture));
	}

	//모든 lecture 조회
	@GetMapping("/api/v1/lectures")
	public ResponseEntity<List<LectureResponse>> findAllLectures(List<Long> lectureIds) {
		return ResponseEntity.ok(lectureRepository.findAllById(lectureIds).stream().map(LectureResponse::from).toList());

	}
}