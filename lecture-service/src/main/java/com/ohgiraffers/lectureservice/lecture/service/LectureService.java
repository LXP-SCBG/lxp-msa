package com.ohgiraffers.lectureservice.lecture.service;

import com.ohgiraffers.lectureservice.common.exception.BusinessException;
import com.ohgiraffers.lectureservice.common.exception.ErrorCode;
import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import com.ohgiraffers.lectureservice.lecture.domain.LectureStatus;
import com.ohgiraffers.lectureservice.lecture.domain.Member;
import com.ohgiraffers.lectureservice.lecture.dto.ContentResponse;
import com.ohgiraffers.lectureservice.lecture.dto.LectureDetailResponse;
import com.ohgiraffers.lectureservice.lecture.dto.LectureListResponse;
import com.ohgiraffers.lectureservice.lecture.repository.ContentRepository;
import com.ohgiraffers.lectureservice.lecture.repository.LectureRepository;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@Transactional(readOnly = true)
public class LectureService {

	private static final Logger log = LoggerFactory.getLogger(LectureService.class);

	private static final String UNKNOWN_INSTRUCTOR_NICKNAME = "강사 정보 없음";
	private static final String DELETED_INSTRUCTOR_NICKNAME = "탈퇴한 강사";

	private final LectureRepository lectureRepository;
	private final ContentRepository contentRepository;
	private final RestClient memberRestClient;


	public LectureService(
		LectureRepository lectureRepository,
		ContentRepository contentRepository,
		RestClient memberRestClient
	) {
		this.lectureRepository = lectureRepository;
		this.contentRepository = contentRepository;
		this.memberRestClient = memberRestClient;
	}

	public List<LectureListResponse> findAllLectures() {
		log.info("공개 강의 목록 조회 시작");
		List<Lecture> lectures = lectureRepository.findByStatusOrderByCreatedAtDesc(
			LectureStatus.PUBLIC
		);
		log.debug("공개 강의 {}건 조회됨", lectures.size());

		//instructor의 membeid, loginid, nickname
		log.info("회원 서비스 호출 - 강사 정보 조회 (GET /api/v1/members)");
		List<Member> members = memberRestClient.get()
				.uri("/api/v1/members")
				.retrieve()
				.body(new ParameterizedTypeReference<List<Member>>() {});
		log.debug("회원 서비스로부터 강사 후보 {}명 수신", members == null ? 0 : members.size());

		return lectures.stream()
			.map(lecture -> LectureListResponse.of(
				lecture,
				members.stream()
					.filter(member -> member.memberId().equals(lecture.getInstructorId()))
					.map(Member::nickname)
					.findFirst()
					.orElse(UNKNOWN_INSTRUCTOR_NICKNAME)
			))
			.toList();
	}

	public LectureDetailResponse findLecture(Long lectureId) {
		log.info("강의 상세 조회 시작 - lectureId={}", lectureId);
		Lecture lecture = lectureRepository.findById(lectureId)
			.orElseThrow(() -> {
				log.warn("강의를 찾을 수 없음 - lectureId={}", lectureId);
				return new BusinessException(ErrorCode.LECTURE_NOT_FOUND);
			});

		if (lecture.getStatus() != LectureStatus.PUBLIC) {
			log.warn("공개되지 않은 강의 접근 시도 - lectureId={}, status={}", lectureId, lecture.getStatus());
			throw new BusinessException(ErrorCode.LECTURE_NOT_ACCESSIBLE);
		}

		//role=insturctor인 member의 nickname
		log.info("회원 서비스 호출 - 강사 정보 조회 (GET /api/v1/members/{}) instructorId={}",
				lecture.getInstructorId(), lecture.getInstructorId());
		Member member = memberRestClient.get()
				.uri("/api/v1/members/{id}", lecture.getInstructorId())
				.retrieve()
				.body(Member.class);

		log.debug("DB 조회 - 강의 콘텐츠 조회 lectureId={}", lectureId);
		List<ContentResponse> contents = contentRepository
			.findByLectureIdOrderBySortOrderAscIdAsc(lectureId)
			.stream()
			.map(ContentResponse::from)
			.toList();
		log.info("강의 상세 조회 완료 - lectureId={}, 콘텐츠 {}건", lectureId, contents.size());

		return LectureDetailResponse.of(
			lecture,
			member.nickname(),
			contents
		);
	}
}