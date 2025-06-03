package org.example.ctrlu.domain.user.entity;

import org.example.ctrlu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String nickname;

	private String image;

	private String verifyToken;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private UserStatus status;

	@Builder
	public User(String email, String password, String nickname, String image, String verifyToken) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.image = image;
		this.verifyToken = verifyToken;
		this.status = UserStatus.NONCERTIFIED;
	}

	public void restore(String password, String nickname, String image, String verifyToken) {
		this.password = password;
		this.nickname = nickname;
		this.image = image;
		this.verifyToken = verifyToken;
		this.status = UserStatus.NONCERTIFIED;
	}

	public void updateStatus(UserStatus status) {
		this.status = status;
	}

	public void updatePassword(String password) {
		this.password = password;
	}
}
