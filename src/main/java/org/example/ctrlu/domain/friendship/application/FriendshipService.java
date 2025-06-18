package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipService {
	private static final int MAX_FRIENDS = 20;

	private final FriendShipRepository friendShipRepository;
	private final UserRepository userRepository;

	@Transactional
	public void requestFriendship(Long userId, FriendshipRequest request) {
		User loginUser = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_USER));

		User target = userRepository.findByIdAndStatus(request.targetId(), UserStatus.ACTIVE)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_TARGET));

		if (Objects.equals(userId, request.targetId())) {
			throw new FriendshipException(CANNOT_FRIEND_SELF);
		}

		if (friendShipRepository.findAcceptedFriendIds(userId).size() >= MAX_FRIENDS) {
			throw new FriendshipException(FRIEND_LIMIT_EXCEEDED);
		}

		Optional<Friendship> currentFriendship = friendShipRepository.findFriendshipBetween(loginUser, target);
		if (currentFriendship.isPresent()) {
			switch (currentFriendship.get().getStatus()) {
				case PENDING -> throw new FriendshipException(ALREADY_REQUESTED_FRIENDSHIP);
				case ACCEPTED -> throw new FriendshipException(ALREADY_ACCEPTED_FRIENDSHIP);
				case REJECTED -> {
					if (currentFriendship.get().getRejectedAt().plusDays(7).isAfter(LocalDateTime.now())){
						throw new FriendshipException(REJECTED_FRIENDSHIP);
					}
					friendShipRepository.delete(currentFriendship.get());
					friendShipRepository.flush();
				}
			}
		}

		try {
			friendShipRepository.save(
				Friendship.builder()
					.fromUser(loginUser)
					.toUser(target)
					.build()
			);
		} catch (DataIntegrityViolationException e) {
			// DB Unique 제약조건 위반 시 발생하는 예외 처리
			// 예: 동일한 fromUser-toUser 쌍으로 이미 데이터가 존재할 경우
			throw new FriendshipException(ALREADY_EXISTS_FRIENDSHIP);
		}
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
			friendShipRepository.findByIdAndToUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		if (friendShipRepository.findAcceptedFriendIds(userId).size() >= MAX_FRIENDS) {
			throw new FriendshipException(FRIEND_LIMIT_EXCEEDED);
		}

		friendship.accept();
	}

	@Transactional
	public void rejectFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendShipRepository.findByIdAndToUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendship.reject();
	}

	@Transactional
	public void cancelFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendShipRepository.findByIdAndFromUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendShipRepository.delete(friendship);
	}
}
