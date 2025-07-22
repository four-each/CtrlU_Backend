package org.example.ctrlu.domain.friendship.application;

import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.dto.response.FriendResponse;
import org.example.ctrlu.domain.friendship.dto.response.GetFriendshipListResponse;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
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

	private final FriendshipRepository friendshipRepository;
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
		if (friendshipRepository.findAcceptedFriendIds(userId).size() >= MAX_FRIENDS) {
			throw new FriendshipException(FRIEND_LIMIT_EXCEEDED);
		}

		validateFriendRequestHistory(loginUser, target);

		try {
			friendshipRepository.save(
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

	private void validateFriendRequestHistory(User loginUser, User target) {
		/**
		 * Pending -> 대기중인 친구 요청이 있습니다.
		 * Accepted -> 이미 친구입니다.
		 * Rejected (target이 거절한 경우) -> 거절 후 일주일이 안지남 -> 요청을 거절한 친구입니다.
		 * Rejected (target이 거절한 경우) -> 거절 후 일주일이 지남 -> 요청 가능
		 * Rejected (LoginUser가 거절한 경우) -> 요청 가능
		 */
		Optional<Friendship> currentFriendship = friendshipRepository.findFriendshipBetween(loginUser, target);
		if (currentFriendship.isPresent()) {
			Friendship friendship = currentFriendship.get();
			switch (friendship.getStatus()) {
				case PENDING -> throw new FriendshipException(ALREADY_REQUESTED_FRIENDSHIP);
				case ACCEPTED -> throw new FriendshipException(ALREADY_ACCEPTED_FRIENDSHIP);
				case REJECTED -> {
					if (Objects.equals(friendship.getToUser().getId(), target.getId()) 
						&& friendship.getRejectedAt().plusDays(7).isAfter(LocalDateTime.now())){
						throw new FriendshipException(REJECTED_FRIENDSHIP);
					}
					friendshipRepository.delete(friendship);
					friendshipRepository.flush();
				}
			}
		}
	}

	@Transactional
	public void deleteFriendship(Long userId, Long friendshipId) {
		Friendship friendship = friendshipRepository.findAcceptedFriendshipById(friendshipId, userId)
			.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendshipRepository.delete(friendship);
	}

	@Transactional
	public void acceptFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendshipRepository.findByIdAndToUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		if (friendshipRepository.findAcceptedFriendIds(userId).size() >= MAX_FRIENDS) {
			throw new FriendshipException(FRIEND_LIMIT_EXCEEDED);
		}

		friendship.accept();
	}

	@Transactional
	public void rejectFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendshipRepository.findByIdAndToUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendship.reject();
	}

	@Transactional
	public void cancelFriendship(Long userId, Long friendshipId) {
		Friendship friendship =
			friendshipRepository.findByIdAndFromUserIdAndStatus(friendshipId, userId, FriendshipStatus.PENDING)
				.orElseThrow(() -> new FriendshipException(NOT_FOUND_FRIENDSHIP));

		friendshipRepository.delete(friendship);
	}

	@Transactional
	public Integer deleteExpiredRejections() {
		return friendshipRepository.deleteByStatusAndRejectedAtBefore(FriendshipStatus.REJECTED, LocalDateTime.now().minusWeeks(1));
	}

	public GetFriendshipListResponse getFriends(Long userId) {
		List<FriendResponse> friends = friendshipRepository.getFriendsOf(userId);
		return GetFriendshipListResponse.from(friends);
	}

	public GetFriendshipListResponse getReceivedRequests(Long userId) {
		List<FriendResponse> receivedRequests = friendshipRepository.getReceivedRequestsOf(userId);
		return GetFriendshipListResponse.from(receivedRequests);
	}

	public GetFriendshipListResponse getSentRequests(Long userId) {
		List<FriendResponse> sentRequests = friendshipRepository.getSentRequestsOf(userId);
		return GetFriendshipListResponse.from(sentRequests);
	}
}
