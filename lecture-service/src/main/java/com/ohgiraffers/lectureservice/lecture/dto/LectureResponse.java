package com.ohgiraffers.lectureservice.lecture.dto;

import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import com.ohgiraffers.lectureservice.lecture.domain.LectureStatus;

public record LectureResponse(
        Long lectureId,
        Integer maxEnrollment
) {
        public static LectureResponse from(Lecture lecture) {
            return new LectureResponse(
                    lecture.getId(),
                    lecture.getMaxEnrollment()
            );
        }
    }