package com.ohgiraffers.memberservice.member.domain;

/**
 * 회원 상태.
 * <ul>
 *   <li>{@link #ACTIVE}  - 활성 회원</li>
 *   <li>{@link #DELETED} - 탈퇴 회원 (soft delete)</li>
 * </ul>
 */
public enum MemberStatus {
    ACTIVE,
    DELETED
}
