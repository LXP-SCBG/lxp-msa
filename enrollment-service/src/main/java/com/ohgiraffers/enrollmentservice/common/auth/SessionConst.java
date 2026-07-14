package com.ohgiraffers.enrollmentservice.common.auth;

/**
 * 세션에 저장하는 속성 키 모음.
 *
 * <p>로그인 상태는 서버 세션(HttpSession) + 브라우저 쿠키(JSESSIONID)로 관리한다.
 * 세션에는 회원 식별자(memberId)만 저장하고, Member 객체나 비밀번호는 저장하지 않는다.
 */
public final class SessionConst {

    /** 로그인한 회원의 memberId 를 담는 세션 키. */
    public static final String LOGIN_MEMBER_ID = "LOGIN_MEMBER_ID";

    private SessionConst() {
    }
}
