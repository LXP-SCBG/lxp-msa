package com.ohgiraffers.memberservice.member.service;

import com.ohgiraffers.memberservice.common.exception.BusinessException;
import com.ohgiraffers.memberservice.common.exception.ErrorCode;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.domain.MemberStatus;
import com.ohgiraffers.memberservice.member.dto.LoginRequest;
import com.ohgiraffers.memberservice.member.dto.SignupRequest;
import com.ohgiraffers.memberservice.member.repository.MemberRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 회원가입.
     */
    @Transactional
    public Member signup(SignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_LOGIN_ID);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Member member = Member.create(request.loginId(), passwordHash, request.nickname());
        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            // existsByLoginId 통과와 save 사이에 동일 아이디가 먼저 저장된 경우(동시성)
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_LOGIN_ID);
        }
    }

    /**
     * 회원탈퇴 (soft delete).
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.withdraw();
    }

    /**
     * 현재 로그인한 회원 조회 (활성 회원만). 세션 복원(GET /auth/me) 등에 사용한다.
     * 세션은 있으나 그 사이 탈퇴한 회원이면 MEMBER_NOT_FOUND.
     */
    @Transactional(readOnly = true)
    public Member findActiveMember(Long memberId) {
        return memberRepository.findByMemberIdAndStatus(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 로그인 인증. 성공 시 회원을 반환한다.
     */
    @Transactional(readOnly = true)
    public Member authenticate(LoginRequest request) {
        Member member = memberRepository
                .findByLoginIdAndStatus(request.loginId(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.MEMBER_INVALID_CREDENTIALS);
        }
        return member;
    }
}
