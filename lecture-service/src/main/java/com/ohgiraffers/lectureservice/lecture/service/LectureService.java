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

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@Transactional(readOnly = true)
public class LectureService {

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
		List<Lecture> lectures = lectureRepository.findByStatusOrderByCreatedAtDesc(
			LectureStatus.PUBLIC
		);

		//instructor의 membeid, loginid, nickname
		List<Member> members = memberRestClient.get()
				.uri("/api/v1/members")
				.retrieve()
				.body(new ParameterizedTypeReference<List<Member>>() {});

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
		Lecture lecture = lectureRepository.findById(lectureId)
			.orElseThrow(() -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND));

		if (lecture.getStatus() != LectureStatus.PUBLIC) {
			throw new BusinessException(ErrorCode.LECTURE_NOT_ACCESSIBLE);
		}

		//role=insturctor인 member의 nickname
		Member member = memberRestClient.get()
				.uri("/api/v1/members/{id}", lecture.getInstructorId())
				.retrieve()
				.body(Member.class);

		List<ContentResponse> contents = contentRepository
			.findByLectureIdOrderBySortOrderAscIdAsc(lectureId)
			.stream()
			.map(ContentResponse::from)
			.toList();

		return LectureDetailResponse.of(
			lecture,
			member.nickname(),
			contents
		);
	}
}