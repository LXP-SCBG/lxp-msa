package com.ohgiraffers.memberservice.member.controller;

import com.ohgiraffers.memberservice.common.auth.LoginMember;
import com.ohgiraffers.memberservice.common.auth.SessionConst;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.dto.LoginRequest;
import com.ohgiraffers.memberservice.member.dto.MemberResponse;
import com.ohgiraffers.memberservice.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 인증 API.
 *
 * <ul>
 *   <li>POST /auth/login  로그인</li>
 *   <li>POST /auth/logout 로그아웃</li>
 *   <li>GET  /auth/me     현재 로그인한 회원 조회 (세션 복원용)</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "JSESSIONID";

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 로그인. 성공 시 세션 생성 후 회원 정보를 반환한다.
     */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        Member member = memberService.authenticate(request);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getMemberId());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    /**
     * 현재 로그인한 회원 정보 조회. 새로고침 시 클라이언트가 세션 상태를 복원하는 데 사용한다.
     * 비로그인이면 {@code @LoginMember} 리졸버가 401(MEMBER_LOGIN_REQUIRED)을 던진다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@LoginMember Long memberId) {
        return ResponseEntity.ok(MemberResponse.from(memberService.findActiveMember(memberId)));
    }

    /**
     * 로그아웃. 세션을 무효화하고 세션 쿠키를 만료시킨다.
     * 세션이 이미 없어도 정상(200) 응답한다(멱등 처리).
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        httpResponse.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}
