package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipRedissonLockService {
	private final FriendshipService friendshipService;
	private final RedissonClient redissonClient;
	private final UserRepository userRepository;
	private final FriendshipRepository friendshipRepository;
	private static final int MAX_FRIENDS = 10;
	private static final long LOCK_WAIT_TIME_SECONDS = 3;
	private static final long LOCK_LEASE_TIME_SECONDS = 5;

	public void requestFriendshipWithLock(Long userId, FriendshipRequest request) {
		User loginUser = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));
		User target = userRepository.findByIdAndStatus(request.targetId(), UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_TARGET));
		if (Objects.equals(userId, request.targetId())) {
			throw new FriendshipException(CANNOT_FRIEND_SELF);
		}
		if (friendshipRepository.findAcceptedFriendIds(userId).size() >= MAX_FRIENDS) {
			throw new FriendshipException(FRIEND_LIMIT_EXCEEDED);
		}

		String lockKey = getFriendshipLockKey(loginUser.getId(), target.getId());
		RLock lock = redissonClient.getLock(lockKey);
		try {
			boolean isLocked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);

			if (!isLocked) {
				// 락 획득 실패: 이미 다른 요청이 이 친구 관계를 처리 중
				throw new FriendshipException(FRIENDSHIP_REQUEST_CONFLICT);
			}

			friendshipService.requestFriendship(userId, request);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new FriendshipException(FRIENDSHIP_REQUEST_LOCK_ACQUISITION_FAILED);
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock(); // 락 해제
			}
		}
	}

	private String getFriendshipLockKey(Long userId1, Long userId2) {
		// 항상 작은 ID를 앞에 두어 (A, B)와 (B, A) 요청이 동일한 락을 공유하도록 정규화
		if (userId1 < userId2) {
			return "friendship-request:" + userId1 + "-" + userId2;
		} else {
			return "friendship-request:" + userId2 + "-" + userId1;
		}
	}
}
