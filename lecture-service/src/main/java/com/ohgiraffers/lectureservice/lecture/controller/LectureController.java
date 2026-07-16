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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping
public class LectureController {

	private static final Logger log = LoggerFactory.getLogger(LectureController.class);

	private final LectureService lectureService;
	private final LectureRepository lectureRepository;

	public LectureController(LectureService lectureService, LectureRepository lectureRepository) {
		this.lectureService = lectureService;
		this.lectureRepository = lectureRepository;
	}

	@GetMapping("/lectures")
	public ResponseEntity<List<LectureListResponse>> findAllLectures() {
		log.info("전체 강의 목록 조회 요청");
		List<LectureListResponse> lectures = lectureService.findAllLectures();
		log.info("전체 강의 목록 조회 완료 - {}건", lectures.size());
		return ResponseEntity.ok(lectures);
	}

	@GetMapping("/lectures/{lectureId}")
	public ResponseEntity<LectureDetailResponse> findLecture(
			@PathVariable Long lectureId
	) {
		log.info("강의 상세 조회 요청 - lectureId={}", lectureId);
		LectureDetailResponse lecture = lectureService.findLecture(lectureId);
		log.info("강의 상세 조회 완료 - lectureId={}", lectureId);
		return ResponseEntity.ok(lecture);
	}

	@GetMapping("/api/v1/lectures/{id}")
	public ResponseEntity<LectureResponse> findLectureById(@PathVariable Long id) {
		log.info("강의 단건 조회 요청(내부 API) - id={}", id);
		Lecture lecture = lectureRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("강의를 찾을 수 없음 - id={}", id);
					return new BusinessException(ErrorCode.LECTURE_NOT_FOUND);
				});

		if (lecture.getStatus() != LectureStatus.PUBLIC) {
			log.warn("공개되지 않은 강의 접근 시도 - id={}, status={}", id, lecture.getStatus());
			throw new BusinessException(ErrorCode.LECTURE_NOT_ACCESSIBLE);
		}
		log.info("강의 단건 조회 완료(내부 API) - id={}", id);
		return ResponseEntity.ok(LectureResponse.from(lecture));
	}

	//모든 lecture 조회
	@GetMapping("/api/v1/lectures")
	public ResponseEntity<List<LectureResponse>> findAllLectures(List<Long> lectureIds) {
		log.info("강의 다건 조회 요청(내부 API) - lectureIds={}", lectureIds);
		List<LectureResponse> lectures = lectureRepository.findAllById(lectureIds).stream()
				.map(LectureResponse::from)
				.toList();
		log.info("강의 다건 조회 완료(내부 API) - 요청 {}건, 응답 {}건",
				lectureIds == null ? 0 : lectureIds.size(), lectures.size());
		return ResponseEntity.ok(lectures);
	}
}