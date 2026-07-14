package com.ohgiraffers.enrollmentservice.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 공통 에러 응답 형식.
 *
 * <p>{@code message} 는 항상 내려가고, {@code fieldErrors} 는 입력 형식 검증 실패 시에만 채워진다.
 * 프론트엔드는 fieldErrors 의 key(필드명)에 맞춰 각 입력창 아래에 메시지를 표시할 수 있다.
 *
 * <pre>{@code
 * {
 *   "message": "입력값을 확인해 주세요.",
 *   "fieldErrors": { "loginId": "아이디는 4~20자로 입력해 주세요." }
 * }
 * }</pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String message;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(String message) {
        this(message, null);
    }

    public ErrorResponse(String message, Map<String, String> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
