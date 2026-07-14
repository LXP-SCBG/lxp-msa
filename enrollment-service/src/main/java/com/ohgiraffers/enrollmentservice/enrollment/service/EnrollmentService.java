package com.ohgiraffers.enrollmentservice.enrollment.service;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Lecture;
import com.ohgiraffers.enrollmentservice.enrollment.dto.EnrollmentResponse;
import com.ohgiraffers.enrollmentservice.enrollment.repository.EnrollmentRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;


@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final RestClient lectureRestClient;
    private final RestClient memberRestClient;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            RestClient lectureRestClient,
            RestClient memberRestClient,
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRestClient = lectureRestClient;
        this.memberRestClient = memberRestClient;
    }

    /**
     * 수강 신청.
     *
     * <p>검증 규칙
     * <ul>
     *   <li>활성 회원만 수강 신청할 수 있다.</li>
     *   <li>존재하지 않는 강의는 신청할 수 없다.</li>
     *   <li>PUBLIC 상태의 강의만 신청할 수 있다.</li>
     *   <li>동일 회원은 동일 강의를 중복 신청할 수 없다.</li>
     * </ul>
     */
    @Transactional
    public Enrollment enroll(Long memberId, Long lectureId) {
        validateActiveMember(memberId);

        Lecture lecture = lectureRestClient.get()
                .uri("api/v1/lectures/{id}", lectureId)
                .retrieve()
                .body(Lecture.class);


        if (enrollmentRepository.existsByMemberIdAndLectureId(memberId, lectureId)) {
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }

        try {
            return enrollmentRepository.save(Enrollment.create(memberId, lectureId));
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEnrollmentException(e)) {
                throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }

            throw e;
        }
    }

    /**
     * 본인 수강 전체 조회.
     *
     * <p>활성 회원만 본인 수강 목록을 조회할 수 있다.
     */
/*    public List<EnrollmentResponse> getMyEnrollments(Long memberId) {
        validateActiveMember(memberId);

        List<Enrollment> enrollments =
            enrollmentRepository.findAllByMemberIdOrderByEnrolledAtDesc(memberId);

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<Long> lectureIds = enrollments.stream()
            .map(Enrollment::getLectureId)
            .toList();

        List<Lecture> lectures = lectureRestClient.get()
                .uri("api/v1/lectures", lectureIds)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Lecture>>() {});

        return enrollments.stream()
            .map(enrollment -> EnrollmentResponse.of(
                enrollment,
                lectureMap.get(enrollment.getLectureId())
            ))
            .toList();
    }*/

    private void validateActiveMember(Long memberId) {

        Member member = memberRestClient.get()
                .uri("/api/v1/members/{id}/active", memberId)
                .retrieve()
                .body(Member.class);

        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
    }

    private boolean isDuplicateEnrollmentException(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        return lowerMessage.contains("enrollment")
            && lowerMessage.contains("member")
            && lowerMessage.contains("lecture");
    }
}