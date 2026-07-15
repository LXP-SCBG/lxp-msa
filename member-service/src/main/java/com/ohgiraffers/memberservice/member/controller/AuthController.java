package com.ohgiraffers.memberservice.member.controller;

import com.ohgiraffers.memberservice.common.auth.LoginMember;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.dto.LoginRequest;
import com.ohgiraffers.memberservice.member.dto.MemberResponse;
import com.ohgiraffers.memberservice.member.service.MemberService;
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
 * <p>세션 관리는 게이트웨이 책임이다. 여기서는 자격 증명만 검증하고
 * 회원 정보를 돌려준다. 게이트웨이가 이 응답의 memberId로 세션을 만든다.
 *
 * <ul>
 *   <li>POST /auth/login  자격 증명 검증 (게이트웨이가 호출)</li>
 *   <li>GET  /auth/me     현재 로그인한 회원 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 로그인 자격 증명 검증. 성공 시 회원 정보를 반환한다.
     * 세션 생성은 게이트웨이가 한다.
     */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request) {
        Member member = memberService.authenticate(request);
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    /**
     * 현재 로그인한 회원 정보 조회. 새로고침 시 클라이언트가 로그인 상태를 복원하는 데 사용한다.
     * 비로그인이면 {@code @LoginMember} 리졸버가 401(MEMBER_LOGIN_REQUIRED)을 던진다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@LoginMember Long memberId) {
        return ResponseEntity.ok(MemberResponse.from(memberService.findActiveMember(memberId)));
    }
}
