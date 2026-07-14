package com.ohgiraffers.memberservice.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    MEMBER_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "사용 불가능한 아이디입니다."),
    MEMBER_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    MEMBER_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 기능입니다."),

    // Lecture
    LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강의입니다."),
    LECTURE_NOT_ACCESSIBLE(HttpStatus.NOT_FOUND, "조회할 수 없는 강의입니다."),

    // Enrollment
    ENROLLMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 수강 중인 강의입니다."),
    ENROLLMENT_LIST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 수강 목록만 조회할 수 있습니다."),
    ENROLLMENT_LECTURE_NOT_ACCESSIBLE(HttpStatus.NOT_FOUND, "해당 강의가 비공개 처리되었습니다."),

    // Goal
    GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 목표입니다."),
    GOAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 목표만 처리할 수 있습니다."),
    GOAL_DETAIL_REQUIRED(HttpStatus.BAD_REQUEST, "세부목표는 최소 1개 이상이어야 합니다."),
    GOAL_DETAIL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "세부목표는 최대 20개까지 등록할 수 있습니다."),
    GOAL_DETAIL_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 목표에 존재하지 않는 세부목표입니다."),

    // Question
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 질문입니다."),
    QUESTION_DELETED(HttpStatus.NOT_FOUND, "삭제된 질문입니다."),
    QUESTION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // Review
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 후기입니다."),
    REVIEW_NOT_ENROLLED(HttpStatus.FORBIDDEN, "수강 신청한 강의만 후기를 작성할 수 있습니다."),
    REVIEW_LIKE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "좋아요를 누를 수 없는 후기입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
