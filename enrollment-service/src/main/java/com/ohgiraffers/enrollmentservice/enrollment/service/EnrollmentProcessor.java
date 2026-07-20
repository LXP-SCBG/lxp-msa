package com.ohgiraffers.enrollmentservice.enrollment.service;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Lecture;
import com.ohgiraffers.enrollmentservice.enrollment.repository.EnrollmentRepository;
import com.ohgiraffers.enrollmentservice.enrollment.repository.LectureSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수강 신청 임계 구역 전용 컴포넌트.
 *
 * <p>비관적 락 획득부터 저장까지를 하나의 트랜잭션으로 묶는다.
 * {@link EnrollmentService#enroll}의 오케스트레이션(REST 검증, 잠금 행 확보)은
 * 트랜잭션 밖에서 수행되어야 하므로, 임계 구역만 선언적 {@code @Transactional}로
 * 감싸기 위해 별도 빈으로 분리했다(같은 빈 내부 호출은 프록시를 타지 않는다).
 */
@Component
public class EnrollmentProcessor {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentProcessor.class);

    private final EnrollmentRepository enrollmentRepository;
    private final LectureSeatRepository lectureSeatRepository;

    public EnrollmentProcessor(
            EnrollmentRepository enrollmentRepository,
            LectureSeatRepository lectureSeatRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lectureSeatRepository = lectureSeatRepository;
    }

    /**
     * 임계 구역: 같은 강의의 수강 신청은 잠금 행의 비관적 락에서 직렬화되어
     * 정원 검증(count)과 저장(insert) 사이의 레이스가 발생하지 않는다.
     *
     * <p>호출 전에 잠금 행(lecture_seats)이 존재해야 하며, 트랜잭션(커넥션 점유)
     * 밖에서 호출해야 한다. 바깥 트랜잭션이 커넥션을 쥔 채 이 트랜잭션이
     * 두 번째 커넥션을 기다리는 풀 고갈 데드락을 막기 위함이다.
     */
    @Transactional
    public Enrollment enroll(Long memberId, Lecture lecture) {
        Long lectureId = lecture.lectureId();

        lectureSeatRepository.findWithLock(lectureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR));

        if (enrollmentRepository.existsByMemberIdAndLectureId(memberId, lectureId)) {
            log.warn("중복 수강신청 차단 - memberId={}, lectureId={}", memberId, lectureId);
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }

        validateEnrollmentCapacity(lecture);

        try {
            Enrollment saved = enrollmentRepository.save(Enrollment.create(memberId, lectureId));
            log.info("수강신청 저장 완료 - enrollmentId={}, memberId={}, lectureId={}",
                    saved.getId(), memberId, lectureId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEnrollmentException(e)) {
                log.warn("동시성 중복 수강신청 감지 - memberId={}, lectureId={}", memberId, lectureId);
                throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }

            log.error("수강신청 저장 실패 - memberId={}, lectureId={}", memberId, lectureId, e);
            throw e;
        }
    }

    /** 수강 정원 초과 여부 검증 */
    private void validateEnrollmentCapacity(Lecture lecture) {
        long currentEnrollment = enrollmentRepository.countByLectureId(lecture.lectureId());

        if (currentEnrollment >= lecture.maxEnrollment()) {
            log.warn("수강 정원 초과 차단 - lectureId={}, current={}, max={}",
                    lecture.lectureId(), currentEnrollment, lecture.maxEnrollment());
            throw new BusinessException(ErrorCode.ENROLLMENT_CAPACITY_EXCEEDED);
        }
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
