package org.example.ctrlu.domain.friendship.entity;

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
public class Friendship extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private FriendshipStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_user_id", nullable = false)
	private User fromUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_user_id", nullable = false)
	private User toUser;

	@Builder
	public Friendship(User fromUser, User toUser) {
		this.fromUser = fromUser;
		this.toUser = toUser;
		this.status = FriendshipStatus.PENDING;
	}

	public void accept() {
		this.status = FriendshipStatus.ACCEPTED;
	}
}
