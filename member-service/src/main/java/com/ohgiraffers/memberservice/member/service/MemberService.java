package com.ohgiraffers.memberservice.member.service;

import com.ohgiraffers.memberservice.common.exception.BusinessException;
import com.ohgiraffers.memberservice.common.exception.ErrorCode;
import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.domain.MemberRole;
import com.ohgiraffers.memberservice.member.domain.MemberStatus;
import com.ohgiraffers.memberservice.member.dto.LoginRequest;
import com.ohgiraffers.memberservice.member.dto.SignupRequest;
import com.ohgiraffers.memberservice.member.repository.MemberRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class MemberService {

    /** 회원 도메인 로거. 가입·인증·탈퇴 같은 주요 이벤트를 남긴다. */
    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

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
            // 중복 아이디 시도: 봇/이상 트래픽 감지에 쓰일 수 있어 warn으로 남긴다
            log.warn("[signup] 중복 loginId 가입 시도: loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.MEMBER_DUPLICATE_LOGIN_ID);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Member member = Member.create(request.loginId(), passwordHash, request.nickname());
        try {
            Member saved = memberRepository.save(member);
            // 가입 성공: 회원 생성 이력 추적용
            log.info("[signup] 가입 완료: memberId={} loginId={}", saved.getMemberId(), saved.getLoginId());
            return saved;
        } catch (DataIntegrityViolationException e) {
            // existsByLoginId 통과와 save 사이에 동일 아이디가 먼저 저장된 경우(동시성)
            log.warn("[signup] 동시성 중복 감지: loginId={}", request.loginId());
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
        // 탈퇴 처리 이력: soft delete라 데이터는 남지만 상태 전환 시점을 기록해 둔다
        log.info("[withdraw] 회원 탈퇴 처리: memberId={}", memberId);
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
     * 강사 목록 조회. 내부 API(/api/v1/members)에서 사용한다.
     */
    @Transactional(readOnly = true)
    public List<Member> findInstructors() {
        return memberRepository.findAllByRole(MemberRole.INSTRUCTOR);
    }

    /**
     * 회원 단건 조회 (상태 무관). 내부 API에서 사용한다.
     */
    @Transactional(readOnly = true)
    public Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 로그인 인증. 성공 시 회원을 반환한다.
     */
    @Transactional(readOnly = true)
    public Member authenticate(LoginRequest request) {
        Member member = memberRepository
                .findByLoginIdAndStatus(request.loginId(), MemberStatus.ACTIVE)
                .orElseThrow(() -> {
                    // 존재하지 않거나 탈퇴한 아이디. 응답은 계정/비번을 구분하지 않지만 로그로는 구분해 둔다
                    log.warn("[login] 존재하지 않는 loginId 시도: loginId={}", request.loginId());
                    return new BusinessException(ErrorCode.MEMBER_INVALID_CREDENTIALS);
                });
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            // 비밀번호 불일치. 비밀번호 값 자체는 절대 로그에 남기지 않는다
            log.warn("[login] 비밀번호 불일치: loginId={}", request.loginId());
            throw new BusinessException(ErrorCode.MEMBER_INVALID_CREDENTIALS);
        }
        // 로그인 성공: 인증 이벤트 추적용
        log.info("[login] 인증 성공: memberId={}", member.getMemberId());
        return member;
    }
}
