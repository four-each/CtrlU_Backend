package org.example.ctrlu.domain.user.repository;

import java.util.Optional;

import org.example.ctrlu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByEmail(String email);
	Optional<User> findByVerifyToken(String verifyToken);

	@Query("SELECT u.image FROM User u WHERE u.id = :id")
    String getImageById(Long id);
}