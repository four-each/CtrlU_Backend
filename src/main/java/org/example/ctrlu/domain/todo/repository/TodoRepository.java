package org.example.ctrlu.domain.todo.repository;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo,Long> {
    boolean existsByUserIdAndEndImageIsNull(Long userId);
}
