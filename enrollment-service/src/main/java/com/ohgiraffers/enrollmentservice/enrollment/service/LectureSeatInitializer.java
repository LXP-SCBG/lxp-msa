package com.ohgiraffers.enrollmentservice.enrollment.service;

import com.ohgiraffers.enrollmentservice.enrollment.repository.LectureSeatRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강의 잠금 행(lecture_seats) 생성 전용 컴포넌트.
 *
 * <p>INSERT IGNORE 를 수강 신청 트랜잭션 안에서 실행하면, 중복 키 체크 과정에서
 * 잡힌 공유 락(S)이 트랜잭션 끝까지 유지되어 이어지는 FOR UPDATE(X락 요청)와
 * 교착 상태(deadlock)를 일으킨다. 별도 트랜잭션(REQUIRES_NEW)에서 실행하고
 * 즉시 커밋해 공유 락을 바로 해제하기 위해 분리했다.
 */
@Component
public class LectureSeatInitializer {

    private final LectureSeatRepository lectureSeatRepository;

    public LectureSeatInitializer(LectureSeatRepository lectureSeatRepository) {
        this.lectureSeatRepository = lectureSeatRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createIfAbsent(Long lectureId) {
        lectureSeatRepository.insertIgnore(lectureId);
    }
}
