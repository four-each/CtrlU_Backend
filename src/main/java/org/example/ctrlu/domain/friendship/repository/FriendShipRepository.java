package org.example.ctrlu.domain.friendship.repository;

import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FriendShipRepository extends JpaRepository<Friendship,Long> {
    List<Long> findAcceptedFriendIds(long userId);
}
