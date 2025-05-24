package org.example.ctrlu.domain.user.repository;

import org.example.ctrlu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
