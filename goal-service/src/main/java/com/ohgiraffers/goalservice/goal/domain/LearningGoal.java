package com.ohgiraffers.goalservice.goal.domain;

import com.ohgiraffers.goalservice.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;


@Entity
@Table(name = "learning_goals")
@SQLDelete(sql = "UPDATE learning_goals SET deleted_at = NOW() WHERE learning_goal_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class LearningGoal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_goal_id")
    private Long learningGoalId;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "title", length = 30, nullable = false)
    private String title;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 삭제일(soft delete). 살아있는 목표는 null. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected LearningGoal() {
    }

    private LearningGoal(Long memberId, String title) {
        this.memberId = memberId;
        this.title = title;
    }

    public static LearningGoal create(Long memberId, String title) {
        validateMemberId(memberId);
        validateTitle(title);
        return new LearningGoal(memberId, title);
    }

    public void updateTitle(String newTitle) {
        validateTitle(newTitle);
        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }

    /** 작성자 본인 여부 확인 */
    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    /** 생성일 기준 24시간 초과 시 만료 */
    public boolean isExpired() {
        return getCreatedAt().plusHours(24).isBefore(LocalDateTime.now());
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId는 null일 수 없습니다.");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수값입니다.");
        }
        if (title.length() > 30) {
            throw new IllegalArgumentException("title은 30자 이하이어야 합니다.");
        }
    }

    public Long getLearningGoalId() {
        return learningGoalId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
