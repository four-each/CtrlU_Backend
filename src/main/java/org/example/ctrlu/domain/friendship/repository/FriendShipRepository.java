package org.example.ctrlu.domain.friendship.repository;

import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
    List<Long> findAcceptedFriendIds(long userId);
}
