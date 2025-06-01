package org.example.ctrlu.domain.todo.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.global.entity.BaseEntity;

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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TodoStatus status;

	@Builder
	public Todo(String title, String startImage, LocalTime challengeTime, User user) {
		this.title = title;
		this.startImage = startImage;
		this.endImage = null;
		this.challengeTime = challengeTime;
		this.durationTime = null;
		this.user = user;
		this.status = TodoStatus.IN_PROGRESS;
	}

	public void complete(int durationTime, String endImageUrl) {
		this.durationTime = durationTime;
		this.endImage = endImageUrl;
		this.status = TodoStatus.COMPLETED;
	}

	public void giveUp() {
		this.status = TodoStatus.GIVEN_UP;
	}

	public void delete() {
		this.status = TodoStatus.DELETED;
	}
}
