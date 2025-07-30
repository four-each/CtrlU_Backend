package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipOptimisticLockService {
	private final FriendshipService friendshipService;

	public void acceptFriendship(Long userId, Long friendshipId) {
		try {
			friendshipService.acceptFriendship(userId, friendshipId);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new FriendshipException(FRIENDSHIP_ACCEPT_CONFLICT);
		}
	}

	public void rejectFriendship(Long userId, Long friendshipId) {
		try {
			friendshipService.rejectFriendship(userId, friendshipId);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new FriendshipException(FRIENDSHIP_REJECT_CONFLICT);
		}
	}

	public void cancelFriendship(Long userId, Long friendshipId) {
		try {
			friendshipService.cancelFriendship(userId, friendshipId);
		} catch (ObjectOptimisticLockingFailureException e) {
			throw new FriendshipException(FRIENDSHIP_CANCEL_CONFLICT);
		}
	}
}
