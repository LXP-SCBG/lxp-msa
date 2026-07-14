package com.ohgiraffers.memberservice.member.domain;

import com.ohgiraffers.memberservice.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 회원 컨텍스트의 Aggregate Root.
 *
 * <p>핵심 불변식 (도메인 모델링 4-3 기준)
 * <ul>
 *   <li>loginId 는 시스템 전체에서 UNIQUE 하며 변경할 수 없다.</li>
 *   <li>passwordHash 는 반드시 해시된 값으로 저장된다 (평문 저장 금지).</li>
 *   <li>가입일(createdAt)은 변경할 수 없다. (BaseEntity 관리)</li>
 *   <li>회원 탈퇴는 soft delete 로 처리한다 (status=DELETED, deletedAt 기록).</li>
 * </ul>
 *
 * <p>형식 검증(아이디 4~20자 등)과 중복 검증은 서비스 계층의 책임이며,
 * 이 엔티티는 도메인 차원의 최소 방어와 상태 전이 규칙만 담당한다.
 */
@Entity
@Table(
        name = "members",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_login_id", columnNames = "login_id")
)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    /** 로그인 아이디. UNIQUE, 변경 불가. */
    @Column(name = "login_id", length = 100, nullable = false, updatable = false, unique = true)
    private String loginId;

    /** BCrypt 등으로 해시된 비밀번호. 평문 저장 금지. */
    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "nickname", length = 100, nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MemberStatus status;

    /** 탈퇴일. 활성 회원은 null. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    protected Member() {
        // JPA 전용 기본 생성자
    }

    private Member(String loginId, String passwordHash, String nickname) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.status = MemberStatus.ACTIVE;
        this.role = MemberRole.STUDENT;
    }

    /**
     * 회원 가입용 팩토리 메서드.
     *
     * @param passwordHash 반드시 해시된 비밀번호 (서비스 계층에서 인코딩 후 전달)
     */
    public static Member create(String loginId, String passwordHash, String nickname) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("loginId는 null이거나 비어 있을 수 없습니다.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash는 null이거나 비어 있을 수 없습니다.");
        }
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("nickname은 null이거나 비어 있을 수 없습니다.");
        }
        return new Member(loginId, passwordHash, nickname);
    }

    /**
     * 회원 탈퇴 (soft delete). Phase 3에서 사용.
     * 멱등 처리: 이미 탈퇴한 회원은 상태를 다시 바꾸지 않는다.
     */
    public void withdraw() {
        if (this.status == MemberStatus.DELETED) {
            return;
        }
        this.status = MemberStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.status == MemberStatus.ACTIVE;
    }

    // loginId 변경 메서드는 불변식상 제공하지 않는다. 가입일은 BaseEntity.createdAt 으로 관리.
    // 비밀번호 변경(회원 정보 수정)은 MVP 제외이므로 setter 미제공.

    public Long getMemberId() {
        return memberId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
