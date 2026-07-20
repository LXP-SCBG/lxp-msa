package com.ohgiraffers.enrollmentservice.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 강의별 수강 정원 잠금 행.
 *
 * <p>데이터가 아니라 뮤텍스 역할을 하는 테이블이다. 수강 신청 트랜잭션이
 * 이 행을 비관적 락(SELECT ... FOR UPDATE)으로 잠가, 같은 강의에 대한
 * 정원 검증(count)과 수강 저장(insert)을 직렬화한다.
 */
@Entity
@Table(name = "lecture_seats")
public class LectureSeat {

    @Id
    @Column(name = "lecture_id")
    private Long lectureId;

    protected LectureSeat() {
    }

    public Long getLectureId() {
        return lectureId;
    }
}
