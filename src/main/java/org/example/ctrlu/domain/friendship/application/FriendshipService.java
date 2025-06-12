package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.entity.Friendship;
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

	@Transactional
	public void requestFriendship(Long userId, FriendshipRequest request) {
		User loginUser = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));

		User target = userRepository.findByIdAndStatus(request.targetId(), UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));

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
		Friendship friendship = friendShipRepository.findByIdAndToUserAndStatus_Pending(friendshipId, userId)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendship.accept();
	}
}
