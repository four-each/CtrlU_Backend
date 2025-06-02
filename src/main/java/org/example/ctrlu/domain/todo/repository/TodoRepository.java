package org.example.ctrlu.domain.todo.repository;

import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo,Long> {
    Page<Todo> findAllByUserIdAndStatus(Long userId, TodoStatus status, Pageable pageable);
    List<Todo> findAllByUserIdAndStatus(Long userId, TodoStatus status);
    Page<Todo> findAllByUserIdInAndStatus(List<Long> friendIds, TodoStatus status, Pageable pageable);

    @Query(value = """
    SELECT *
    FROM (
        SELECT t.*, ROW_NUMBER() OVER (PARTITION BY t.user_id ORDER BY t.created_at DESC) AS rn
        FROM todo t 
        WHERE t.user_id IN (:friendIds) 
          AND t.created_at >= :since 
          AND t.status <> 'GIVEN_UP'
    ) ranked
    WHERE ranked.rn = 1 
    ORDER BY ranked.created_at DESC 
    LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Todo> findPagedLatestTodoPerFriend(
            @Param("friendIds") List<Long> friendIds,
            @Param("since") LocalDateTime since,
            @Param("limit") int limit,
            @Param("offset") int offset);



    @Query(value = """
    SELECT COUNT(*) FROM (
        SELECT user_id 
        FROM todo 
        WHERE user_id IN (:friendIds) 
          AND created_at >= :since 
          AND status <> 'GIVEN_UP'
        GROUP BY user_id
    ) AS grouped
    """, nativeQuery = true)
    int countFriendsWithRecentTodos(
            @Param("friendIds") List<Long> friendIds,
            @Param("since") LocalDateTime since);


    List<Todo> findByUserIdAndCreatedAtAfterAndStatusNot(Long userId, LocalDateTime createdAtAfter, TodoStatus status);
}
