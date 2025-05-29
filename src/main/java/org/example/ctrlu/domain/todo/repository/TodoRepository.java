package org.example.ctrlu.domain.todo.repository;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TodoRepository extends JpaRepository<Todo,Long> {
    Page<Todo> findAllByUserIdAndStatus(Long userId, TodoStatus status, Pageable pageable);
    List<Todo> findAllByUserIdAndStatus(Long userId, TodoStatus status);
    Page<Todo> findAllByUserIdInAndStatus(List<Long> friendIds, TodoStatus status, Pageable pageable);
}
