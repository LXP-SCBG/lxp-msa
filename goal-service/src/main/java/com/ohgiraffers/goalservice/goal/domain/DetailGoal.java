package com.ohgiraffers.goalservice.goal.domain;

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
@Table(name = "detail_goals")
@SQLDelete(sql = "UPDATE detail_goals SET deleted_at = NOW() WHERE detail_goal_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DetailGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_goal_id")
    private Long detailGoalId;

    @Column(name = "learning_goal_id", nullable = false, updatable = false)
    private Long learningGoalId;

    @Column(name = "content", length = 50, nullable = false)
    private String content;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 삭제일(soft delete). 살아있는 세부목표는 null. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected DetailGoal() {
    }

    private DetailGoal(Long learningGoalId, String content, int sortOrder) {
        this.learningGoalId = learningGoalId;
        this.content = content;
        this.completed = false;
        this.sortOrder = sortOrder;
    }

    public static DetailGoal create(Long learningGoalId, String content, int sortOrder) {
        validateLearningGoalId(learningGoalId);
        validateContent(content);
        validateSortOrder(sortOrder);
        return new DetailGoal(learningGoalId, content, sortOrder);
    }

    public void complete() {
        this.completed = true;
    }

    /** 달성 여부를 토글(체크/해제)한다. */
    public void changeCompletion(boolean completed) {
        this.completed = completed;
    }

    /** 내용과 노출 순서를 변경한다. 완료 상태(completed)는 유지한다. */
    public void update(String content, int sortOrder) {
        validateContent(content);
        validateSortOrder(sortOrder);
        this.content = content;
        this.sortOrder = sortOrder;
    }

    private static void validateLearningGoalId(Long learningGoalId) {
        if (learningGoalId == null) {
            throw new IllegalArgumentException("learningGoalId는 null일 수 없습니다.");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content는 필수값입니다.");
        }
        if (content.length() > 50) {
            throw new IllegalArgumentException("content는 50자 이하이어야 합니다.");
        }
    }

    private static void validateSortOrder(int sortOrder) {
        if (sortOrder < 1) {
            throw new IllegalArgumentException("sortOrder는 1 이상이어야 합니다.");
        }
    }

    public Long getDetailGoalId() {
        return detailGoalId;
    }

    public Long getLearningGoalId() {
        return learningGoalId;
    }

    public String getContent() {
        return content;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}
