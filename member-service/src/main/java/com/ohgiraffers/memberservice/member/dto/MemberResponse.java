package com.ohgiraffers.memberservice.member.dto;


import com.ohgiraffers.memberservice.member.domain.Member;
import com.ohgiraffers.memberservice.member.domain.MemberStatus;

/**
 * 회원 정보 응답. 비밀번호 해시는 절대 포함하지 않는다.
 */
public record MemberResponse(Long memberId, String loginId, String nickname, MemberStatus status) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getMemberId(), member.getLoginId(), member.getNickname(), member.getStatus());
    }
}
