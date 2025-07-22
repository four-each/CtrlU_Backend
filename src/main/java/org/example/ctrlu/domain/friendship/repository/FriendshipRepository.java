package org.example.ctrlu.domain.friendship.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.example.ctrlu.domain.friendship.dto.response.FriendResponse;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship,Long> {
    @Query("""
    SELECT CASE
               WHEN f.fromUser.id = :userId THEN f.toUser.id
               ELSE f.fromUser.id
           END
    FROM Friendship f
    WHERE (f.fromUser.id = :userId OR f.toUser.id = :userId)
      AND f.status = 'ACCEPTED'
    """)
    List<Long> findAcceptedFriendIds(@Param("userId") Long userId);

    @Query("""
        SELECT f
        FROM Friendship f
        WHERE (f.fromUser = :loginUser AND f.toUser = :target)
           OR (f.fromUser = :target AND f.toUser = :loginUser)
    """)
    Optional<Friendship> findFriendshipBetween(@Param("loginUser") User loginUser, @Param("target") User target);

    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.id = :friendshipId
            AND (f.fromUser.id = :userId OR f.toUser.id = :userId)
            AND f.status = 'ACCEPTED'
    """)
    Optional<Friendship> findAcceptedFriendshipById(Long friendshipId, Long userId);
    Optional<Friendship> findByIdAndToUserIdAndStatus(Long friendshipId, Long toUserId, FriendshipStatus status);
    Optional<Friendship> findByIdAndFromUserIdAndStatus(Long friendshipId, Long fromUserId, FriendshipStatus status);
    Integer deleteByStatusAndRejectedAtBefore(FriendshipStatus friendshipStatus, LocalDateTime localDateTime);

    @Query("""
        SELECT NEW org.example.ctrlu.domain.friendship.dto.response.FriendResponse(
            CASE
                WHEN f.fromUser.id = :userId THEN f.toUser.id
                WHEN f.toUser.id = :userId THEN f.fromUser.id
            END,
            CASE
                WHEN f.fromUser.id = :userId THEN f.toUser.nickname ELSE f.fromUser.nickname
            END,
            CASE
                WHEN f.fromUser.id = :userId THEN f.toUser.email ELSE f.fromUser.email
            END,
            CASE
                WHEN f.fromUser.id = :userId THEN f.toUser.image ELSE f.fromUser.image
            END
        )
        FROM Friendship f
        WHERE f.status = 'ACCEPTED'
            AND (f.fromUser.id = :userId OR f.toUser.id = :userId)
        ORDER BY f.createdAt desc
    """)
	List<FriendResponse> getFriendsOf(Long userId);

    @Query("""
        SELECT NEW org.example.ctrlu.domain.friendship.dto.response.FriendResponse(
            f.fromUser.id, f.fromUser.nickname, f.fromUser.email, f.fromUser.image
        )
        FROM Friendship f
        WHERE f.status = 'PENDING'
            AND f.toUser.id = :userId
        ORDER BY f.createdAt desc
    """)
    List<FriendResponse> getReceivedRequestsOf(Long userId);

    @Query("""
        SELECT NEW org.example.ctrlu.domain.friendship.dto.response.FriendResponse(
            f.toUser.id, f.toUser.nickname, f.toUser.email, f.toUser.image
        )
        FROM Friendship f
        WHERE f.status = 'PENDING'
            AND f.fromUser.id = :userId
        ORDER BY f.createdAt desc
    """)
    List<FriendResponse> getSentRequestsOf(Long userId);
}
