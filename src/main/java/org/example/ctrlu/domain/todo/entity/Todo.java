package org.example.ctrlu.domain.todo.entity;

import java.time.LocalTime;

import lombok.Builder;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String startImage;

	private String endImage;

	@Column(nullable = false)
	private LocalTime challengeTime;

	private Integer durationTime;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Builder
	public Todo(String title, String startImage, LocalTime challengeTime, User user) {
		this.title = title;
		this.startImage = startImage;
		this.endImage = null;
		this.challengeTime = challengeTime;
		this.durationTime = null;
		this.user = user;
	}
}
