package com.ohgiraffers.lectureservice.lecture.domain;

import com.ohgiraffers.lectureservice.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "lectures")
public class Lecture extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "lecture_id")
	private Long id;

	@Column(name = "instructor_id", nullable = false)
	private Long instructorId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LectureStatus status;

	protected Lecture() {
	}

	public Long getId() {
		return id;
	}

	public Long getInstructorId() {
		return instructorId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public LectureStatus getStatus() {
		return status;
	}
}