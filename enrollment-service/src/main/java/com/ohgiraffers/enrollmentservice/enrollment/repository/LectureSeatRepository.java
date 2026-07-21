package com.ohgiraffers.enrollmentservice.enrollment.repository;

import com.ohgiraffers.enrollmentservice.enrollment.domain.LectureSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureSeatRepository extends JpaRepository<LectureSeat, Long> {

    /**
     * 카운터 행이 없으면 잔여석 초기값(= max_enrollment)으로 생성한다(있으면 무시).
     *
     * <p>최초 신청 두 건이 동시에 들어와도 PK 충돌로 한 건만 삽입되므로 안전하다.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO lecture_seats (lecture_id, remaining_seats) VALUES (:lectureId, :remainingSeats)", nativeQuery = true)
    void insertIgnore(@Param("lectureId") Long lectureId, @Param("remainingSeats") int remainingSeats);

    /**
     * 잔여석이 남아 있을 때만 좌석을 1 차감한다(원자적 재고 감소).
     *
     * <p>{@code WHERE remaining_seats > 0} 가드 덕분에 아무리 많은 요청이
     * 동시에 들어와도 잔여석은 0 미만으로 내려가지 않는다(오버셀 0). 단일 UPDATE 라
     * SELECT ... FOR UPDATE 로 임계 구역을 잡거나 COUNT(*) 로 인원을 세지 않으므로,
     * 신청이 쌓여도 처리 시간이 일정하게 유지된다.
     *
     * @return 차감된 행 수. 1 이면 좌석 확보 성공, 0 이면 정원 초과.
     */
    @Modifying
    @Query(value = "UPDATE lecture_seats SET remaining_seats = remaining_seats - 1 "
            + "WHERE lecture_id = :lectureId AND remaining_seats > 0", nativeQuery = true)
    int decrementRemainingSeats(@Param("lectureId") Long lectureId);
}
