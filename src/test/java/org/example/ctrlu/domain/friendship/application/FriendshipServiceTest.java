package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;
import static org.mockito.BDDMockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
public class FriendshipServiceTest {
	@InjectMocks
	private FriendshipService friendshipService;

	@Mock
	private FriendshipRepository friendshipRepository;

	private User loginUser;
	private User targetUser;
	private static final int MAX_FRIENDS = 20;

	@BeforeEach
	void setUp() {
		loginUser = User.builder()
			.nickname("loginUser")
			.email("test@test.com")
			.password("password")
			.image("image")
			.build();
		ReflectionTestUtils.setField(loginUser, "id", 1L);
		ReflectionTestUtils.setField(loginUser, "status", UserStatus.ACTIVE);

		targetUser = User.builder()
			.nickname("targetUser")
			.email("test@test.com")
			.password("password")
			.image("image")
			.build();
		ReflectionTestUtils.setField(targetUser, "id", 2L);
		ReflectionTestUtils.setField(targetUser, "status", UserStatus.ACTIVE);
	}

	@Nested
	@DisplayName("친구 삭제 테스트")
	class DeleteFriendshipTests {
		@Test
		@DisplayName("친구 삭제에 성공한다.")
		void deleteFriendship_Success() {
			// given
			Long friendshipId = 1L;
			Friendship acceptedFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(acceptedFriendship, "status", FriendshipStatus.ACCEPTED);
			given(friendshipRepository.findAcceptedFriendshipById(friendshipId, loginUser.getId()))
				.willReturn(Optional.of(acceptedFriendship));

			// when
			friendshipService.deleteFriendship(loginUser.getId(), friendshipId);

			// then
			verify(friendshipRepository, times(1)).delete(acceptedFriendship);
		}

		@Test
		@DisplayName("친구 관계가 존재하지 않을 경우 예외가 발생한다.")
		void deleteFriendship_Fail_NotFound() {
			// given
			Long nonExistentFriendshipId = 1L;
			when(friendshipRepository.findAcceptedFriendshipById(nonExistentFriendshipId, loginUser.getId()))
				.thenReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.deleteFriendship(loginUser.getId(), nonExistentFriendshipId))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_FRIENDSHIP.getMessage());
			verify(friendshipRepository, never()).delete(any());
		}
	}

	@Nested
	@DisplayName("친구 요청 수락 테스트")
	class AcceptFriendshipTests {
		@Test
		@DisplayName("친구 요청 수락에 성공한다.")
		void acceptFriendship_Success() {
			// given
			Long friendshipId = 1L;
			Friendship pendingFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(pendingFriendship, "status", FriendshipStatus.PENDING);
			given(friendshipRepository.findByIdAndToUserIdAndStatus(friendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.of(pendingFriendship));
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId()))
				.willReturn(Collections.emptyList());

			// when
			friendshipService.acceptFriendship(loginUser.getId(), friendshipId);

			// then
			assertThat(pendingFriendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
		}

		@Test
		@DisplayName("존재하지 않는 친구 요청일 경우 예외가 발생한다.")
		void acceptFriendship_Fail_NotFound() {
			// given
			Long nonExistentFriendshipId = 99L;
			given(friendshipRepository.findByIdAndToUserIdAndStatus(nonExistentFriendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.acceptFriendship(loginUser.getId(), nonExistentFriendshipId))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_FRIENDSHIP.getMessage());
		}

		@Test
		@DisplayName("친구 수가 최대 한도에 도달한 경우 예외가 발생한다.")
		void acceptFriendship_Fail_FriendLimitExceeded() {
			// given
			Long friendshipId = 1L;
			List<Long> fullFriends = Collections.nCopies(MAX_FRIENDS, 0L);
			Friendship pendingFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(pendingFriendship, "status", FriendshipStatus.PENDING);
			given(friendshipRepository.findByIdAndToUserIdAndStatus(friendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.of(pendingFriendship));
			given(friendshipRepository.findAcceptedFriendIds(loginUser.getId()))
				.willReturn(fullFriends);

			// when & then
			assertThatThrownBy(() -> friendshipService.acceptFriendship(loginUser.getId(), friendshipId))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(FRIEND_LIMIT_EXCEEDED.getMessage());
		}
	}

	@Nested
	@DisplayName("친구 요청 거절 테스트")
	class RejectFriendshipTests {
		@Test
		@DisplayName("친구 요청 거절에 성공한다.")
		void rejectFriendship_Success() {
			// given
			Long friendshipId = 1L;
			Friendship pendingFriendship = Friendship.builder()
				.fromUser(targetUser)
				.toUser(loginUser)
				.build();
			ReflectionTestUtils.setField(pendingFriendship, "status", FriendshipStatus.PENDING);
			given(friendshipRepository.findByIdAndToUserIdAndStatus(friendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.of(pendingFriendship));

			// when
			friendshipService.rejectFriendship(loginUser.getId(), friendshipId);

			// then
			assertThat(pendingFriendship.getStatus()).isEqualTo(FriendshipStatus.REJECTED);
		}

		@Test
		@DisplayName("존재하지 않거나 내가 받은 친구 요청이 아닌 경우 예외가 발생한다.")
		void rejectFriendship_Fail_NotFound() {
			// given
			Long nonExistentFriendshipId = 99L;
			given(friendshipRepository.findByIdAndToUserIdAndStatus(nonExistentFriendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.rejectFriendship(loginUser.getId(), nonExistentFriendshipId))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_FRIENDSHIP.getMessage());
		}
	}

	@Nested
	@DisplayName("보낸 친구 요청 취소 테스트")
	class CancelFriendshipTests {
		@Test
		@DisplayName("보낸 친구 요청 취소에 성공한다.")
		void cancelFriendship_Success() {
			// given
			Long friendshipId = 1L;
			Friendship sentFriendship = Friendship.builder()
				.fromUser(loginUser)
				.toUser(targetUser)
				.build();
			given(friendshipRepository.findByIdAndFromUserIdAndStatus(friendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.of(sentFriendship));

			// when
			friendshipService.cancelFriendship(loginUser.getId(), friendshipId);

			// then
			verify(friendshipRepository, times(1)).delete(sentFriendship);
		}

		@Test
		@DisplayName("존재하지 않거나 내가 보낸 요청이 아니면 예외가 발생한다.")
		void cancelFriendship_Fail_NotFound() {
			// given
			Long nonExistentFriendshipId = 99L;
			given(friendshipRepository.findByIdAndFromUserIdAndStatus(nonExistentFriendshipId, loginUser.getId(), FriendshipStatus.PENDING))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> friendshipService.cancelFriendship(loginUser.getId(), nonExistentFriendshipId))
				.isInstanceOf(FriendshipException.class)
				.hasMessage(NOT_FOUND_FRIENDSHIP.getMessage());
			verify(friendshipRepository, never()).delete(any());
		}
	}
}
