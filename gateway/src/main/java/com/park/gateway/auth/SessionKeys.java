package com.park.gateway.auth;

/**
 * 게이트웨이 세션 속성 키와 내부 전달 헤더.
 *
 * <p>세션의 주인은 게이트웨이다. 게이트웨이는 WebSession에 memberId를 저장하고,
 * 프록시할 때 그 값을 X-Member-Id 헤더로 각 서비스에 넘긴다.
 */
public final class SessionKeys {

    /** 게이트웨이 WebSession에 저장하는 로그인 회원 id 속성 키. */
    public static final String MEMBER_ID = "MEMBER_ID";

    /** 내부 서비스로 전달하는 헤더. 서비스의 @LoginMember 리졸버가 읽는다. */
    public static final String MEMBER_ID_HEADER = "X-Member-Id";

    private SessionKeys() {
    }
}
