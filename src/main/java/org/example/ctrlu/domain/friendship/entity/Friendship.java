package org.example.ctrlu.domain.friendship.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_friendship_users",
			columnNames = {"user1Id", "user2Id"}
		)
	}
)
public class Friendship extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private FriendshipStatus status;

	private LocalDateTime rejectedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_user_id", nullable = false)
	private User fromUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_user_id", nullable = false)
	private User toUser;

	@Column(nullable = false, updatable = false)
	private Long user1Id;

	@Column(nullable = false, updatable = false)
	private Long user2Id;

	@Builder
	public Friendship(User fromUser, User toUser) {
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.status = FriendshipStatus.PENDING;

		if (fromUser.getId() < toUser.getId()) {
			this.user1Id = fromUser.getId();
			this.user2Id = toUser.getId();
		} else {
			this.user1Id = toUser.getId();
			this.user2Id = fromUser.getId();
		}
	}

	public void accept() {
		this.status = FriendshipStatus.ACCEPTED;
	}

	public void reject() {
		this.status = FriendshipStatus.REJECTED;
		rejectedAt = LocalDateTime.now();
	}
}
