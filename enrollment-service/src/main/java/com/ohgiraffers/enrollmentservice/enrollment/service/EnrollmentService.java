package com.ohgiraffers.enrollmentservice.enrollment.service;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Lecture;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Member;
import com.ohgiraffers.enrollmentservice.enrollment.grpc.GrpcLectureClient;
import com.ohgiraffers.enrollmentservice.enrollment.repository.LectureSeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final LectureSeatRepository lectureSeatRepository;
    private final LectureSeatInitializer lectureSeatInitializer;
    private final EnrollmentProcessor enrollmentProcessor;
    private final GrpcLectureClient grpcLectureClient;
    private final RestClient memberRestClient;

    public EnrollmentService(
            LectureSeatRepository lectureSeatRepository,
            LectureSeatInitializer lectureSeatInitializer,
            EnrollmentProcessor enrollmentProcessor,
            GrpcLectureClient grpcLectureClient,
            RestClient memberRestClient
    ) {
        this.lectureSeatRepository = lectureSeatRepository;
        this.lectureSeatInitializer = lectureSeatInitializer;
        this.enrollmentProcessor = enrollmentProcessor;
        this.grpcLectureClient = grpcLectureClient;
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
     * 비관적 락 ~ 저장의 임계 구역만 {@link EnrollmentProcessor}의 트랜잭션으로
     * 처리한다. 바깥 트랜잭션이 커넥션을 쥔 채 안쪽 트랜잭션이 두 번째 커넥션을
     * 기다리는 풀 고갈 데드락을 막기 위한 구조이므로 순서를 바꾸면 안 된다.
     */
    public Enrollment enroll(Long memberId, Long lectureId) {
        log.info("수강신청 처리 시작 - memberId={}, lectureId={}", memberId, lectureId);

        validateActiveMember(memberId);

        // 강의 조회를 gRPC로. 없거나 비공개면 클라이언트가 BusinessException으로 변환해 던진다.
        Lecture lecture = grpcLectureClient.getLecture(lectureId);
        log.debug("강의 조회 완료(gRPC) - lectureId={}, maxEnrollment={}", lectureId, lecture.maxEnrollment());

        // 강의별 잠금 행 확보. 임계 구역과 같은 트랜잭션에서 INSERT IGNORE 하면
        // 중복 키 체크의 공유 락(S)이 트랜잭션 끝까지 남아 FOR UPDATE 와 데드락이
        // 나므로, 반드시 임계 구역 밖의 독립 트랜잭션에서 만들고 커밋해 둔다.
        if (!lectureSeatRepository.existsById(lectureId)) {
            lectureSeatInitializer.createIfAbsent(lectureId);
        }

        return enrollmentProcessor.enroll(memberId, lecture);
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
    }
}

