package com.ohgiraffers.enrollmentservice.enrollment.dto;

import com.ohgiraffers.enrollmentservice.enrollment.domain.Enrollment;
import java.time.LocalDateTime;

//public record EnrollmentResponse(
//        Long enrollmentId,
//        Long lectureId,
//        String lectureTitle,
//        LocalDateTime enrolledAt
//) {
//
//    public static EnrollmentResponse of(Enrollment enrollment, Lecture lecture) {
//        return new EnrollmentResponse(
//                enrollment.getId(),
//                enrollment.getLectureId(),
//                lecture != null ? lecture.getTitle() : null,
//                enrollment.getEnrolledAt()
//        );
//    }
//}
