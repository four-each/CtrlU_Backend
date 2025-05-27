package org.example.ctrlu.domain.user.entity;

import java.time.LocalDateTime;

import lombok.Builder;
import org.example.ctrlu.global.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
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

	private LocalDateTime deletedAt;

	public void delete() {
		this.deletedAt = LocalDateTime.now();
	}

	@Builder
	public User(String email, String password, String nickname, String image) {
		this.email = email;
		this.password = password;
		this.nickname = nickname;
		this.image = image;
	}
}
