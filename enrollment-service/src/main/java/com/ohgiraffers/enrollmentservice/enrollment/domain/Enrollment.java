package com.ohgiraffers.enrollmentservice.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * 수강 컨텍스트의 Aggregate Root.
 *
 * <p>핵심 불변식
 * <ul>
 *   <li>수강 신청은 인증된 회원만 할 수 있다. (서비스 계층에서 @LoginMember로 보장)</li>
 *   <li>memberId, lectureId는 필수값이며 변경할 수 없다.</li>
 *   <li>동일 회원은 동일 강의를 중복 신청할 수 없다. (UNIQUE 제약)</li>
 *   <li>enrolledAt은 생성 시 자동 설정되며 변경할 수 없다.</li>
 * </ul>
 *
 * <p>MVP 제외: 수강 취소, 수강 상태 변경, 수강 숨김
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(name = "uq_enrollment", columnNames = {"member_id", "lecture_id"})
)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "lecture_id", nullable = false, updatable = false)
    private Long lectureId;

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    protected Enrollment() {
        // JPA 전용 기본 생성자
    }

    private Enrollment(Long memberId, Long lectureId) {
        this.memberId = memberId;
        this.lectureId = lectureId;
        this.enrolledAt = LocalDateTime.now();
    }

    /**
     * 수강 신청 팩토리 메서드.
     * 검증(강의 공개 여부, 중복 여부)은 서비스 계층에서 수행 후 호출한다.
     */
    public static Enrollment create(Long memberId, Long lectureId) {
        return new Enrollment(memberId, lectureId);
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getLectureId() {
        return lectureId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
}
