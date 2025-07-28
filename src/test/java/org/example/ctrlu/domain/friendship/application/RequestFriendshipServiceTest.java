package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RequestFriendshipServiceTest {
	@InjectMocks
	private FriendshipService friendshipService;
	@Mock
	private FriendshipRepository friendshipRepository;
	@Mock
	private UserRepository userRepository;

	private User loginUser;
	private User targetUser;
	private FriendshipRequest request;
	private static final int MAX_FRIENDS = 20;

	@BeforeEach
	void setUp() {
		loginUser = User.builder()
			.nickname("loginUser")
			.email("test@test.com")
			.password("password")
			.profileImageKey("image")
			.build();
		ReflectionTestUtils.setField(loginUser, "id", 1L);
		ReflectionTestUtils.setField(loginUser, "status", UserStatus.ACTIVE);

		targetUser = User.builder()
			.nickname("targetUser")
			.email("test@test.com")
			.password("password")
			.profileImageKey("image")
			.build();
		ReflectionTestUtils.setField(targetUser, "id", 2L);
		ReflectionTestUtils.setField(targetUser, "status", UserStatus.ACTIVE);

		request = new FriendshipRequest(targetUser.getId());
	}

	private void mockUserFindById() {
		given(userRepository.findByIdAndStatus(loginUser.getId(), UserStatus.ACTIVE)).willReturn(
			Optional.of(loginUser));
		given(userRepository.findByIdAndStatus(targetUser.getId(), UserStatus.ACTIVE)).willReturn(
			Optional.of(targetUser));
	}

	@Nested
	@DisplayName("친구 요청 성공")
	class SuccessCases {
		@Test
		@DisplayName("친구 요청에 성공한다.")
		void requestFriendship_Success() {
			// given
			mockUserFindById();
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(Optional.empty());

			// when
			friendshipService.requestFriendship(loginUser.getId(), request);

			// then
			verify(friendshipRepository, times(1)).save(any(Friendship.class));
		}

		@Test
		@DisplayName("상대방이 요청을 거절했으나 7일이 지나 재요청에 성공한다.")
		void requestFriendship_Success_After7DaysOfRejection() {
			// given
			mockUserFindById();
			Friendship rejectedFriendship = Friendship.builder()
				.fromUser(loginUser)
				.toUser(targetUser)
				.build();
			ReflectionTestUtils.setField(rejectedFriendship, "status", FriendshipStatus.REJECTED);
			ReflectionTestUtils.setField(rejectedFriendship, "rejectedAt", LocalDateTime.now().minusDays(8));

			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(
				Optional.of(rejectedFriendship));

			// when
			friendshipService.requestFriendship(loginUser.getId(), request);

			// then
			verify(friendshipRepository, times(1)).delete(rejectedFriendship);
			verify(friendshipRepository, times(1)).flush();
			verify(friendshipRepository, times(1)).save(any(Friendship.class));
		}

		@Test
		@DisplayName("내가 거절했던 상대에게 친구 요청에 성공한다.")
		void requestFriendship_Success_WhenLoginUserRejectedBefore() {
			// given
			mockUserFindById();
			Friendship rejectedByMe = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(rejectedByMe, "status", FriendshipStatus.REJECTED);
			ReflectionTestUtils.setField(rejectedByMe, "rejectedAt", LocalDateTime.now().minusDays(1));

			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(
				Optional.of(rejectedByMe));

			// when
			friendshipService.requestFriendship(loginUser.getId(), request);

			// then
			verify(friendshipRepository, times(1)).delete(rejectedByMe);
			verify(friendshipRepository, times(1)).flush();
			verify(friendshipRepository, times(1)).save(any(Friendship.class));
		}
	}

	@Nested
	@DisplayName("친구 요청 실패")
	class FailureCases {
		@Test
		@DisplayName("요청하는 유저가 존재하지 않으면 예외가 발생한다")
		void requestFriendship_Fail_LoginUserNotFound() {
			// given
			given(userRepository.findByIdAndStatus(loginUser.getId(), UserStatus.ACTIVE)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_USER.getMessage());
		}

		@Test
		@DisplayName("대상 유저가 존재하지 않으면 예외가 발생한다")
		void requestFriendship_Fail_TargetUserNotFound() {
			// given
			given(userRepository.findByIdAndStatus(loginUser.getId(), UserStatus.ACTIVE)).willReturn(Optional.of(loginUser));
			given(userRepository.findByIdAndStatus(targetUser.getId(), UserStatus.ACTIVE)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_TARGET.getMessage());
		}

		@Test
		@DisplayName("스스로에게 친구 요청을 보낼 수 없다")
		void requestFriendship_Fail_CannotFriendSelf() {
			// given
			FriendshipRequest selfRequest = new FriendshipRequest(loginUser.getId());
			given(userRepository.findByIdAndStatus(loginUser.getId(), UserStatus.ACTIVE)).willReturn(Optional.of(loginUser));

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), selfRequest))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(CANNOT_FRIEND_SELF.getMessage());
		}

		@Test
		@DisplayName("최대 친구 수를 초과하면 예외가 발생한다")
		void requestFriendship_Fail_FriendLimitExceeded() {
			// given
			mockUserFindById();
			List<Long> friendIds = Collections.nCopies(MAX_FRIENDS, 0L);
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(friendIds);

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(FRIEND_LIMIT_EXCEEDED.getMessage());
		}

		@Test
		@DisplayName("이미 친구 요청을 보낸 상태(PENDING)이면 예외가 발생한다")
		void requestFriendship_Fail_AlreadyRequested() {
			// given
			mockUserFindById();
			Friendship pendingFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(pendingFriendship, "status", FriendshipStatus.PENDING);
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(Optional.of(pendingFriendship));

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(ALREADY_REQUESTED_FRIENDSHIP.getMessage());
		}

		@Test
		@DisplayName("이미 친구 관계(ACCEPTED)이면 예외가 발생한다")
		void requestFriendship_Fail_AlreadyFriends() {
			// given
			mockUserFindById();
			Friendship acceptedFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(acceptedFriendship, "status", FriendshipStatus.ACCEPTED);
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(Optional.of(acceptedFriendship));

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(ALREADY_ACCEPTED_FRIENDSHIP.getMessage());
		}

		@Test
		@DisplayName("상대방이 요청을 거절했고 7일이 지나지 않았으면 예외가 발생한다")
		void requestFriendship_Fail_RejectedWithin7Days() {
			// given
			mockUserFindById();
			Friendship rejectedFriendship = Friendship.builder()
				.fromUser(loginUser)
				.toUser(targetUser)
				.build();
			ReflectionTestUtils.setField(rejectedFriendship, "status", FriendshipStatus.REJECTED);
			ReflectionTestUtils.setField(rejectedFriendship, "rejectedAt", LocalDateTime.now().minusDays(1));

			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId())).willReturn(Collections.emptyList());
			given(friendshipRepository.findFriendshipBetween(loginUser, targetUser)).willReturn(Optional.of(rejectedFriendship));

			// when & then
			assertThatThrownBy(() -> friendshipService.requestFriendship(loginUser.getId(), request))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(REJECTED_FRIENDSHIP.getMessage());
		}
	}
}