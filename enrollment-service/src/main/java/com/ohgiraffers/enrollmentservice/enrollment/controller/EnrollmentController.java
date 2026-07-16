package com.ohgiraffers.enrollmentservice.enrollment.controller;

import com.ohgiraffers.enrollmentservice.common.auth.LoginMember;
import com.ohgiraffers.enrollmentservice.enrollment.dto.EnrollRequest;
import com.ohgiraffers.enrollmentservice.enrollment.service.EnrollmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * 수강 신청.
     */
    @PostMapping
    public ResponseEntity<Void> enroll(
        @LoginMember Long memberId,
        @Valid @RequestBody EnrollRequest request
    ) {
        log.info("수강신청 요청 - memberId={}, lectureId={}", memberId, request.lectureId());

        enrollmentService.enroll(memberId, request.lectureId());

        log.info("수강신청 완료 - memberId={}, lectureId={}", memberId, request.lectureId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 본인 수강 전체 조회.
     */
/*    @GetMapping
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(
        @LoginMember Long memberId
    ) {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(memberId));
    }*/
}