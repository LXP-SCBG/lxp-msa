package com.ohgiraffers.enrollmentservice.enrollment.repository;

import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /** 중복 수강 신청 여부 확인 */
    boolean existsByMemberIdAndLectureId(Long memberId, Long lectureId);

    /** 본인의 수강 목록을 신청일 최신순으로 조회 */
    List<Enrollment> findAllByMemberIdOrderByEnrolledAtDesc(Long memberId);
}
