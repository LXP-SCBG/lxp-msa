package com.ohgiraffers.memberservice.member.controller;

import com.ohgiraffers.memberservice.common.auth.LoginMember;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.dto.MemberResponse;
import com.ohgiraffers.memberservice.member.dto.SignupRequest;
import com.ohgiraffers.memberservice.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API (외부 공개용). 세션 관리는 게이트웨이 책임.
 *
 * <p>서비스 간 내부 조회 API는 {@link InternalMemberController}(/api/v1/members)에 있다.
 *
 * <ul>
 *   <li>POST   /members     회원가입 (게이트웨이가 세션 생성으로 자동 로그인 처리)</li>
 *   <li>DELETE /members/me  회원탈퇴 (게이트웨이가 세션 무효화)</li>
 * </ul>
 */
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    /**
     * 회원가입. 세션 생성(자동 로그인)은 게이트웨이가 응답의 memberId로 처리한다.
     */
    @PostMapping
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request) {
        Member member = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    /**
     * 회원탈퇴. 세션 무효화는 게이트웨이가 처리한다.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@LoginMember Long memberId) {
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }
}
