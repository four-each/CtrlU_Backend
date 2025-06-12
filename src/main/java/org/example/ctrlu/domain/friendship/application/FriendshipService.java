package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipService {
	private final FriendShipRepository friendShipRepository;
	private final UserRepository userRepository;

	// 친구 요청 보냈는데 거절 당함 -> 해당 친구에게 다시 요청 불가능
	// 친구 요청 받았는데 거절함 -> 해당 친구에게 다시 요청 가능
	@Transactional
	public void requestFriendship(Long userId, FriendshipRequest request) {
		User loginUser = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));

		User target = userRepository.findByIdAndStatus(request.targetId(), UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));

		if (friendShipRepository.existsFriendshipBy(loginUser, target, FriendshipStatus.PENDING)) {
			throw new FriendshipException(ALREADY_REQUESTED_FRIENDSHIP);
		}

		if (friendShipRepository.existsFriendshipBy(loginUser, target, FriendshipStatus.ACCEPTED)) {
			throw new FriendshipException(FRIENDSHIP_EXISTS);
		}

		if (friendShipRepository.existsRejectedFriendshipBy(loginUser, target)) {
			throw new FriendshipException(REJECTED_FRIENDSHIP);
		}

		Friendship friendship = Friendship.builder()
			.fromUser(loginUser)
			.toUser(target)
			.build();

		friendShipRepository.save(friendship);
	}

	@Transactional
	public void deleteFriendship(Long userId, Long friendshipId) {
		Friendship friendship = friendShipRepository.findAcceptedFriendshipById(friendshipId, userId)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendShipRepository.delete(friendship);
	}

	@Transactional
	public void acceptFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendShipRepository.findByIdAndToUserAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendship.accept();
	}

	@Transactional
	public void rejectFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendShipRepository.findByIdAndToUserAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendship.reject();
	}

	@Transactional
	public void cancelFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendShipRepository.findByIdAndFromUserAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendShipRepository.delete(friendship);
	}
}
