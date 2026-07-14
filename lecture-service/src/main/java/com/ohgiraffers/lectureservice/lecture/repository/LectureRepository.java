package com.ohgiraffers.lectureservice.lecture.repository;

import com.ohgiraffers.lectureservice.lecture.domain.Lecture;
import com.ohgiraffers.lectureservice.lecture.domain.LectureStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

	List<Lecture> findByStatusOrderByCreatedAtDesc(LectureStatus status);
}