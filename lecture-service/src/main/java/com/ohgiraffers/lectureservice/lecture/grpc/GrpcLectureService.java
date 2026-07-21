package com.ohgiraffers.lectureservice.lecture.grpc;

import com.ohgiraffers.lecture.grpc.GetLectureReply;
import com.ohgiraffers.lecture.grpc.GetLectureRequest;
import com.ohgiraffers.lecture.grpc.LectureInternalServiceGrpc;
import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import com.ohgiraffers.lectureservice.lecture.domain.LectureStatus;
import com.ohgiraffers.lectureservice.lecture.repository.LectureRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * 수강 신청 검증용 강의 조회 gRPC 서버.
 *
 * <p>기존 REST 내부 API {@code GET /api/v1/lectures/{id}}(LectureController.findLectureById)와
 * 동일한 로직을 gRPC로 노출한다. 두 경로는 당분간 공존한다(gRPC 검증 후 REST 제거 검토).
 *
 * <p>오류는 응답 필드가 아니라 gRPC 상태 코드로 전달한다.
 * <ul>
 *   <li>강의 없음 → {@link Status#NOT_FOUND}</li>
 *   <li>비공개(PUBLIC 아님) → {@link Status#FAILED_PRECONDITION}</li>
 * </ul>
 * 클라이언트(enrollment)가 이 상태 코드를 자신의 BusinessException으로 되돌린다.
 */
@GrpcService
public class GrpcLectureService extends LectureInternalServiceGrpc.LectureInternalServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(GrpcLectureService.class);

    private final LectureRepository lectureRepository;

    public GrpcLectureService(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }

    @Override
    public void getLecture(GetLectureRequest request, StreamObserver<GetLectureReply> responseObserver) {
        long lectureId = request.getLectureId();
        log.debug("gRPC 강의 조회 요청 - lectureId={}", lectureId);

        Lecture lecture = lectureRepository.findById(lectureId).orElse(null);
        if (lecture == null) {
            log.warn("gRPC 강의 조회 실패(없음) - lectureId={}", lectureId);
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("존재하지 않는 강의입니다. id=" + lectureId)
                    .asRuntimeException());
            return;
        }

        if (lecture.getStatus() != LectureStatus.PUBLIC) {
            log.warn("gRPC 강의 조회 차단(비공개) - lectureId={}, status={}", lectureId, lecture.getStatus());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription("조회할 수 없는 강의입니다. id=" + lectureId)
                    .asRuntimeException());
            return;
        }

        GetLectureReply reply = GetLectureReply.newBuilder()
                .setLectureId(lecture.getId())
                .setMaxEnrollment(lecture.getMaxEnrollment())
                .build();

        // unary 응답: 값을 한 번 보내고(onNext) 스트림을 닫는다(onCompleted).
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
        log.debug("gRPC 강의 조회 완료 - lectureId={}, maxEnrollment={}", lectureId, lecture.getMaxEnrollment());
    }
}
