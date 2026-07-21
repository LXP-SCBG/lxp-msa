package com.ohgiraffers.enrollmentservice.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 강의별 수강 정원 카운터 행(재고).
 *
 * <p>{@code remaining_seats}(잔여 좌석 수)를 들고 있다. 수강 신청은
 * {@code UPDATE ... SET remaining_seats = remaining_seats - 1 WHERE remaining_seats > 0}
 * 단일 원자 UPDATE 로 정원 검증과 좌석 확보를 동시에 처리하므로,
 * SELECT ... FOR UPDATE + COUNT(*) 로 임계 구역을 직렬화할 필요가 없다.
 */
@Entity
@Table(name = "lecture_seats")
public class LectureSeat {

    @Id
    @Column(name = "lecture_id")
    private Long lectureId;

    @Column(name = "remaining_seats", nullable = false)
    private int remainingSeats;

    protected LectureSeat() {
    }

    public Long getLectureId() {
        return lectureId;
    }

    public int getRemainingSeats() {
        return remainingSeats;
    }
}
