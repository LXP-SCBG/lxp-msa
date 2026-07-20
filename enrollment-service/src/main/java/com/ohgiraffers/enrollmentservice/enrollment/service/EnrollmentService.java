package com.ohgiraffers.enrollmentservice.enrollment.service;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Lecture;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Member;
import com.ohgiraffers.enrollmentservice.enrollment.repository.EnrollmentRepository;
import com.ohgiraffers.enrollmentservice.enrollment.repository.LectureSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;


@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final LectureSeatRepository lectureSeatRepository;
    private final LectureSeatInitializer lectureSeatInitializer;
    private final TransactionTemplate transactionTemplate;
    private final RestClient lectureRestClient;
    private final RestClient memberRestClient;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            LectureSeatRepository lectureSeatRepository,
            LectureSeatInitializer lectureSeatInitializer,
            PlatformTransactionManager transactionManager,
            RestClient lectureRestClient,
            RestClient memberRestClient
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.lectureSeatRepository = lectureSeatRepository;
        this.lectureSeatInitializer = lectureSeatInitializer;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
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
     *   <li>수강 정원(maxEnrollment)이 가득 찬 강의는 신청할 수 없다.</li>
     * </ul>
     *
     * <p>트랜잭션 구조: 이 메서드 자체는 트랜잭션이 아니다.
     * REST 검증과 잠금 행 확보는 트랜잭션(커넥션 점유) 밖에서 끝내고,
     * 비관적 락 ~ 저장의 임계 구역만 {@code transactionTemplate}으로 감싼다.
     * 바깥 트랜잭션이 커넥션을 쥔 채 안쪽 트랜잭션이 두 번째 커넥션을
     * 기다리는 풀 고갈 데드락을 막기 위한 구조이므로 순서를 바꾸면 안 된다.
     */
    public Enrollment enroll(Long memberId, Long lectureId) {
        log.info("수강신청 처리 시작 - memberId={}, lectureId={}", memberId, lectureId);

        validateActiveMember(memberId);

        Lecture lecture = lectureRestClient.get()
                .uri("api/v1/lectures/{id}", lectureId)
                .retrieve()
                .body(Lecture.class);

        if (lecture == null) {
            throw new BusinessException(ErrorCode.LECTURE_NOT_FOUND);
        }
        log.debug("강의 조회 완료 - lectureId={}, maxEnrollment={}", lectureId, lecture.maxEnrollment());

        // 강의별 잠금 행 확보. 임계 구역과 같은 트랜잭션에서 INSERT IGNORE 하면
        // 중복 키 체크의 공유 락(S)이 트랜잭션 끝까지 남아 FOR UPDATE 와 데드락이
        // 나므로, 반드시 임계 구역 밖의 독립 트랜잭션에서 만들고 커밋해 둔다.
        if (!lectureSeatRepository.existsById(lectureId)) {
            lectureSeatInitializer.createIfAbsent(lectureId);
        }

        // 임계 구역: 같은 강의의 수강 신청은 잠금 행의 비관적 락에서 직렬화되어
        // 정원 검증(count)과 저장(insert) 사이의 레이스가 발생하지 않는다.
        return transactionTemplate.execute(status -> {
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
        });
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

    /** 수강 정원 초과 여부 검증 */
    private void validateEnrollmentCapacity(Lecture lecture) {
        long currentEnrollment = enrollmentRepository.countByLectureId(lecture.lectureId());

        if (currentEnrollment >= lecture.maxEnrollment()) {
            log.warn("수강 정원 초과 차단 - lectureId={}, current={}, max={}",
                    lecture.lectureId(), currentEnrollment, lecture.maxEnrollment());
            throw new BusinessException(ErrorCode.ENROLLMENT_CAPACITY_EXCEEDED);
        }
    }

    private void validateActiveMember(Long memberId) {

        Member member = memberRestClient.get()
                .uri("/api/v1/members/{id}/active", memberId)
                .retrieve()
                .body(Member.class);

        if (member == null) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
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