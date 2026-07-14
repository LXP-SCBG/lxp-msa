package com.ohgiraffers.enrollmentservice.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollRequest(
        @NotNull(message = "강의 ID는 필수값입니다.")
        Long lectureId
) {
}
