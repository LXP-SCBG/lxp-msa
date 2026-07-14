package com.ohgiraffers.lectureservice.lecture.repository;

import com.ohgiraffers.lectureservice.lecture.domain.Content;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {

	List<Content> findByLectureIdOrderBySortOrderAscIdAsc(Long lectureId);
}