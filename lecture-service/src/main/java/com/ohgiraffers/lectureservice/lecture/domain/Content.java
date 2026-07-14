package com.ohgiraffers.lectureservice.lecture.domain;

import com.ohgiraffers.lectureservice.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "contents")
public class Content extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "content_id")
	private Long id;

	@Column(name = "lecture_id", nullable = false)
	private Long lectureId;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(name = "content_url", nullable = false, length = 500)
	private String contentUrl;

	@Column(name = "sort_order", nullable = false)
	private Integer sortOrder;

	protected Content() {
	}

	public Long getId() {
		return id;
	}

	public Long getLectureId() {
		return lectureId;
	}

	public String getTitle() {
		return title;
	}

	public String getContentUrl() {
		return contentUrl;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}
}