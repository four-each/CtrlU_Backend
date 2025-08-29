package org.example.ctrlu.domain.todo.repository;

import org.example.ctrlu.domain.todo.dto.projection.TodoProjection;
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

    @Query("SELECT new org.example.ctrlu.domain.todo.dto.projection.TodoProjection(" +
            "       t.id, u.nickname, t.createdAt) " +
            "FROM Todo t JOIN t.user u " +
            "WHERE u.id IN :friendIds AND t.status = :status")
    Page<TodoProjection> findTodoProjectionsBy(
            @Param("friendIds") List<Long> friendIds,
            @Param("status") TodoStatus status,
            Pageable pageable
    );

    @Query("SELECT new org.example.ctrlu.domain.todo.dto.projection.TodoProjection(" +
            "       t.id, u.nickname, t.createdAt) " +
            "FROM Todo t JOIN t.user u " +
            "WHERE u.id = :userId AND t.status = :status")
    TodoProjection findTodoProjectionBy(
            @Param("userId") Long userId,
            @Param("status") TodoStatus status
    );

    // @Query 어노테이션에 countQuery를 추가하고, 기존 쿼리에서 LIMIT/OFFSET 제거
    @Query(
            value = """
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
        """,
            countQuery = """
        SELECT COUNT(*) 
        FROM (
            SELECT user_id 
            FROM todo 
            WHERE user_id IN (:friendIds) 
              AND created_at >= :since 
              AND status <> 'GIVEN_UP' 
            GROUP BY user_id
        ) AS grouped
        """,
            nativeQuery = true)
    Page<Todo> findPagedLatestTodoPerFriend(
            @Param("friendIds") List<Long> friendIds,
            @Param("since") LocalDateTime since,
            Pageable pageable);

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

    @Query("""
        SELECT t FROM Todo t
        WHERE t.user.id = :targetId
        AND t.createdAt >= :timeLimit
        AND t.status <> :excludedStatus
        ORDER BY t.createdAt ASC
        """)
    List<Todo> findAllRecentTodosByUserId(
            @Param("targetId") long targetId,
            @Param("timeLimit") LocalDateTime localDateTime,
            @Param("excludedStatus") TodoStatus todoStatus
    );

    boolean existsByUserIdAndCreatedAtAfterAndStatusNot(long userId, LocalDateTime localDateTime, TodoStatus todoStatus);
}
