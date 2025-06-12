package org.example.ctrlu.domain.friendship.repository;

import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendShipRepository extends JpaRepository<Friendship,Long> {
    @Query("""
    SELECT CASE 
               WHEN f.fromUser.id = :userId THEN f.toUser.id 
               ELSE f.fromUser.id 
           END
    FROM Friendship f
    WHERE (f.fromUser.id = :userId OR f.toUser.id = :userId)
      AND f.status = 'ACCEPTED'
    """)
    List<Long> findAcceptedFriendIds(@Param("userId") long userId);


    @Query("""
        SELECT f
        FROM Friendship f
        WHERE f.id = :friendshipId
            AND (f.fromUser.id = :userId OR f.toUser.id = :userId)
            AND f.status = 'ACCEPTED'
    """)
    Optional<Friendship> findAcceptedFriendshipById(Long friendshipId, Long userId);

    Optional<Friendship> findByIdAndToUserAndStatus_Pending(Long friendshipId, Long toUserId);
}
