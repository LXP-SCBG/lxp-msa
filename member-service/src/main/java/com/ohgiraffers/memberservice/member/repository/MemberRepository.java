package com.ohgiraffers.memberservice.member.repository;

import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.domain.MemberRole;
import com.ohgiraffers.memberservice.member.domain.MemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * 회원 레포지토리.
 *
 * <p>다른 도메인(수강·질문·후기 등)은 기본적으로 {@code findById} 로 회원 존재 여부를 확인하면 된다.
 * 인증/조회 단계에서 탈퇴 회원을 걸러야 할 경우 {@code ...AndStatus} 메서드를 사용한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 회원가입 시 loginId 중복 검증용.
     * soft delete 특성상 DELETED 회원의 loginId 도 포함하여 검사한다.
     * (loginId 는 시스템 전체 UNIQUE 불변식 → 탈퇴한 아이디는 재사용 불가)
     */
    boolean existsByLoginId(String loginId);

    /** 로그인 시 loginId 로 회원 조회 (상태 무관). */
    Optional<Member> findByLoginId(String loginId);

    /** 활성 회원만 조회해야 할 때 사용 (예: 탈퇴 회원 인증 차단). */
    Optional<Member> findByLoginIdAndStatus(String loginId, MemberStatus status);

    Optional<Member> findByMemberIdAndStatus(Long memberId, MemberStatus status);

    List<Member> findAllByRole(MemberRole memberRole);
}
