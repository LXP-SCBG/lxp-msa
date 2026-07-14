package com.ohgiraffers.memberservice.member.controller;

import com.ohgiraffers.memberservice.common.auth.LoginMember;
import com.ohgiraffers.memberservice.common.auth.SessionConst;
import com.ohgiraffers.memberservice.common.exception.BusinessException;
import com.ohgiraffers.memberservice.common.exception.ErrorCode;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.domain.MemberRole;
import com.ohgiraffers.memberservice.member.domain.MemberStatus;
import com.ohgiraffers.memberservice.member.dto.MemberResponse;
import com.ohgiraffers.memberservice.member.dto.SignupRequest;
import com.ohgiraffers.memberservice.member.repository.MemberRepository;
import com.ohgiraffers.memberservice.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 API.
 *
 * <ul>
 *   <li>POST   /members     회원가입 (성공 시 자동 로그인)</li>
 *   <li>DELETE /members/me  회원탈퇴 (soft delete 후 세션 무효화)</li>
 * </ul>
 */
@RestController
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    public MemberController(MemberService memberService, MemberRepository memberRepository) {
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    /**
     * 회원가입.
     */
    @PostMapping
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        Member member = memberService.signup(request);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(SessionConst.LOGIN_MEMBER_ID, member.getMemberId());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    /**
     * 회원탈퇴.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(@LoginMember Long memberId,
            HttpServletRequest httpRequest) {
        memberService.withdraw(memberId);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/members")
    public ResponseEntity<List<MemberResponse>> getInstructors() {
        List<MemberResponse> memberResponses = memberRepository.findAllByRole(MemberRole.INSTRUCTOR).stream().
                map(MemberResponse::from).toList();

        return ResponseEntity.ok(memberResponses);
    }

    @GetMapping("/api/v1/members/{id}")
    public ResponseEntity<MemberResponse> getInstructor(@PathVariable Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @GetMapping("/api/v1/members/{id}/active")
    public ResponseEntity<MemberResponse> getActiveMember(@PathVariable Long id) {
        Member member = memberRepository.findByMemberIdAndStatus(id, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
