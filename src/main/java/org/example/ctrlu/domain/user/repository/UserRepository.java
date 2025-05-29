package org.example.ctrlu.domain.user.repository;

import java.util.Optional;

import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	Optional<User> findByEmailAndStatus(String email, UserStatus status);
	Optional<User> findByVerifyToken(String verifyToken);
	Optional<User> findByIdAndStatus(Long userId, UserStatus status);
}