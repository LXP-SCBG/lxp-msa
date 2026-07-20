package com.ohgiraffers.enrollmentservice.enrollment.repository;

import com.ohgiraffers.enrollmentservice.enrollment.domain.LectureSeat;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureSeatRepository extends JpaRepository<LectureSeat, Long> {

    /**
     * 잠금 행이 없으면 생성한다(있으면 무시).
     *
     * <p>최초 신청 두 건이 동시에 들어와도 PK 충돌로 한 건만 삽입되므로 안전하다.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO lecture_seats (lecture_id) VALUES (:lectureId)", nativeQuery = true)
    void insertIgnore(@Param("lectureId") Long lectureId);

    /**
     * 강의 잠금 행을 비관적 락으로 조회한다(SELECT ... FOR UPDATE).
     *
     * <p>트랜잭션 커밋/롤백 시점에 락이 풀리며, 같은 강의의 수강 신청은
     * 이 지점에서 한 줄로 직렬화된다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LectureSeat s WHERE s.lectureId = :lectureId")
    Optional<LectureSeat> findWithLock(@Param("lectureId") Long lectureId);
}
