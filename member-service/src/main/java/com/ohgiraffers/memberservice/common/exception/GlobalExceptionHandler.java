package com.ohgiraffers.memberservice.common.exception;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리. 모든 도메인 컨트롤러의 예외를 일관된 {@link ErrorResponse} 형식으로 변환한다.
 *
 * <p>로그 레벨 규칙:
 * <ul>
 *   <li>비즈니스 예외(예상된 실패) → warn: 흐름 추적용, 스택트레이스 불필요</li>
 *   <li>예상 못 한 예외 → error + 스택트레이스: 코드 결함이므로 반드시 원인 추적</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 전역 예외 로거. 예외 발생 지점과 원인을 남긴다. */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 비즈니스 예외 → ErrorCode 의 상태·메시지로 응답
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e){
        ErrorCode errorCode = e.getErrorCode();
        // 예상된 실패이므로 warn. 어떤 코드가 몇 번 나는지 추적하는 용도
        log.warn("[BusinessException] code={} status={} message={}",
                errorCode.name(), errorCode.getHttpStatus(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.getMessage()));
    }

    /**
     * 입력 형식 검증 실패(@Valid). 필드별 메시지를 함께 내려준다.
     * 예: 회원가입 시 아이디 길이 미달, 비밀번호 누락 등.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            // 한 필드에 여러 제약이 걸리면 첫 메시지만 사용
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        // 검증 실패는 흔한 사용자 실수이므로 warn, 필드 목록만 남긴다
        log.warn("[Validation] fields={}", fieldErrors.keySet());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("입력값을 확인해 주세요.", fieldErrors));
    }

    /** 도메인 불변식 위반 등(예: Member.create 의 방어 로직) → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[IllegalArgument] message={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
    }

    /**
     * 위에서 잡지 못한 모든 예외 → 500.
     * 예상 못 한 예외는 코드 결함이므로 error 레벨로 스택트레이스까지 남긴다.
     * (예: DB 스키마 불일치로 인한 SQL 예외 — 로그가 없으면 원인 추적이 불가능하다)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("[Unexpected] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("서버 내부 오류가 발생했습니다."));
    }
}
