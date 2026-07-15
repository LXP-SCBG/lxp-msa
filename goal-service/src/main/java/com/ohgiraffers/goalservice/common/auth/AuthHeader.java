package com.ohgiraffers.goalservice.common.auth;

/**
 * 게이트웨이 → 서비스로 전달되는 인증 헤더.
 *
 * <p>세션은 게이트웨이가 관리한다. 게이트웨이가 세션에서 memberId를 꺼내
 * 이 헤더에 담아 넘기고, 서비스는 헤더만 읽는다.
 * 외부에서 직접 이 헤더를 보내는 위조는 게이트웨이가 걸러낸다.
 */
public final class AuthHeader {

    public static final String MEMBER_ID = "X-Member-Id";

    private AuthHeader() {
    }
}
