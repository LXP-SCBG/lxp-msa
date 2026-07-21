package com.ohgiraffers.enrollmentservice.enrollment.grpc;

import com.ohgiraffers.enrollmentservice.common.exception.BusinessException;
import com.ohgiraffers.enrollmentservice.common.exception.ErrorCode;
import com.ohgiraffers.enrollmentservice.enrollment.domain.Lecture;
import com.ohgiraffers.lecture.grpc.GetLectureReply;
import com.ohgiraffers.lecture.grpc.GetLectureRequest;
import com.ohgiraffers.lecture.grpc.LectureInternalServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

/**
 * lecture-service의 gRPC 서버를 호출하는 클라이언트. 기존 RestClient(lectureRestClient) 호출을 대체한다.
 *
 * <p>채널은 이름("lecture-service")으로 만들고 blocking 스텁을 재사용한다(채널 생성은 비싸다).
 * 채널 주소는 application.yml의 {@code spring.grpc.client.channel.lecture-service.target}에서 온다.
 *
 * <p>gRPC 상태 코드를 enrollment의 BusinessException으로 되돌려, REST일 때와 겉보기 동작을 맞춘다.
 * <ul>
 *   <li>{@code NOT_FOUND} → {@link ErrorCode#LECTURE_NOT_FOUND}</li>
 *   <li>{@code FAILED_PRECONDITION} → {@link ErrorCode#ENROLLMENT_LECTURE_NOT_ACCESSIBLE}</li>
 *   <li>그 외 → {@link ErrorCode#COMMON_INTERNAL_ERROR}</li>
 * </ul>
 */
@Component
public class GrpcLectureClient {

    private static final Logger log = LoggerFactory.getLogger(GrpcLectureClient.class);

    private final LectureInternalServiceGrpc.LectureInternalServiceBlockingStub stub;

    public GrpcLectureClient(GrpcChannelFactory channels) {
        this.stub = LectureInternalServiceGrpc.newBlockingStub(channels.createChannel("lecture-service"));
    }

    /** 강의 조회. 없거나 비공개면 BusinessException으로 변환해 던진다(정상 반환값은 항상 non-null). */
    public Lecture getLecture(Long lectureId) {
        try {
            GetLectureReply reply = stub.getLecture(
                    GetLectureRequest.newBuilder().setLectureId(lectureId).build());
            return new Lecture(reply.getLectureId(), reply.getMaxEnrollment());
        } catch (StatusRuntimeException e) {
            throw toBusinessException(e, lectureId);
        }
    }

    private BusinessException toBusinessException(StatusRuntimeException e, Long lectureId) {
        Status.Code code = e.getStatus().getCode();
        return switch (code) {
            case NOT_FOUND -> new BusinessException(ErrorCode.LECTURE_NOT_FOUND);
            case FAILED_PRECONDITION -> new BusinessException(ErrorCode.ENROLLMENT_LECTURE_NOT_ACCESSIBLE);
            default -> {
                log.error("gRPC 강의 조회 실패 - lectureId={}, status={}", lectureId, code, e);
                yield new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR);
            }
        };
    }
}
