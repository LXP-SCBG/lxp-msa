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
 * <p>원자적 좌석 차감(UPDATE)부터 저장(INSERT)까지를 하나의 트랜잭션으로 묶는다.
 * {@link EnrollmentService#enroll}의 오케스트레이션(REST 검증, 카운터 행 확보)은
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
     * 임계 구역: 원자적 좌석 차감(UPDATE)으로 정원 검증과 좌석 확보를 한 번에 처리한다.
     *
     * <p>{@code decrementRemainingSeats} 의 {@code WHERE remaining_seats > 0} 가드가
     * 오버셀을 원천 차단하므로, SELECT ... FOR UPDATE 로 임계 구역을 직렬화하거나
     * COUNT(*) 로 인원을 세지 않는다. 이 덕분에 같은 강의에 요청이 몰려도 임계 구역이
     * (차감 UPDATE + INSERT) 로 짧게 유지되고, 신청이 쌓여도 처리 시간이 늘지 않는다.
     *
     * <p>차감 후 INSERT 가 중복 키로 실패하면 {@link BusinessException}(RuntimeException)
     * 을 던져 트랜잭션을 롤백하므로, 방금 차감한 좌석은 자동으로 원복된다(별도 보상 불필요).
     *
     * <p>호출 전에 카운터 행(lecture_seats)이 존재해야 하며, 트랜잭션(커넥션 점유)
     * 밖에서 호출해야 한다. 바깥 트랜잭션이 커넥션을 쥔 채 이 트랜잭션이
     * 두 번째 커넥션을 기다리는 풀 고갈 데드락을 막기 위함이다.
     */
    @Transactional
    public Enrollment enroll(Long memberId, Lecture lecture) {
        Long lectureId = lecture.lectureId();

        if (enrollmentRepository.existsByMemberIdAndLectureId(memberId, lectureId)) {
            log.warn("중복 수강신청 차단 - memberId={}, lectureId={}", memberId, lectureId);
            throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
        }


        // 원자적 재고 감소: 잔여석이 있을 때만 1 차감. 0 이면 정원 초과.
        int updated = lectureSeatRepository.decrementRemainingSeats(lectureId);

        if (updated == 0) {
            log.warn("수강 정원 초과 차단 - lectureId={}", lectureId);
            throw new BusinessException(ErrorCode.ENROLLMENT_CAPACITY_EXCEEDED);
        }

        try {
            Enrollment saved = enrollmentRepository.save(Enrollment.create(memberId, lectureId));
            log.info("수강신청 저장 완료 - enrollmentId={}, memberId={}, lectureId={}",
                    saved.getId(), memberId, lectureId);
            return saved;
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEnrollmentException(e)) {
                // 중복 신청. 트랜잭션 롤백으로 위에서 차감한 좌석이 원복된다.
                log.warn("중복 수강신청 감지 - memberId={}, lectureId={}", memberId, lectureId);
                throw new BusinessException(ErrorCode.ENROLLMENT_ALREADY_EXISTS);
            }

            log.error("수강신청 저장 실패 - memberId={}, lectureId={}", memberId, lectureId, e);
            throw e;
        }
    }

    private boolean isDuplicateEnrollmentException(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();

        if (message == null) {
            return false;
        }

        String lowerMessage = message.toLowerCase();

        return lowerMessage.contains("uq_enrollment");
    }
}
