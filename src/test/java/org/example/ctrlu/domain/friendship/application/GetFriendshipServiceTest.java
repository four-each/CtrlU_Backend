package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.example.ctrlu.domain.friendship.dto.response.FriendResponse;
import org.example.ctrlu.domain.friendship.dto.response.GetFriendshipListResponse;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GetFriendshipServiceTest {
	@InjectMocks
	private FriendshipService friendshipService;
	@Mock
	private FriendshipRepository friendshipRepository;
	private final Long USER_ID = 1L;

	@Test
	@DisplayName("사용자의 친구 목록을 성공적으로 조회한다")
	void getFriends_ShouldReturnFriendsList() {
		// Given
		List<FriendResponse> friends = Arrays.asList(
			new FriendResponse(2L, "친구1", "friend1@example.com", "img1.png"),
			new FriendResponse(3L, "친구2", "friend2@example.com", "img2.png")
		);
		given(friendshipRepository.getFriendsOf(USER_ID)).willReturn(friends);

		// When
		GetFriendshipListResponse response = friendshipService.getFriends(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(2);
		assertThat(response.friends().get(0).nickname()).isEqualTo("친구1");
		assertThat(response.friends().get(1).email()).isEqualTo("friend2@example.com");
	}

	@Test
	@DisplayName("친구가 없을 때 빈 친구 목록을 반환한다")
	void getFriends_ShouldReturnEmptyList_WhenNoFriends() {
		// Given
		when(friendshipRepository.getFriendsOf(USER_ID)).thenReturn(Collections.emptyList());

		// When
		GetFriendshipListResponse response = friendshipService.getFriends(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}

	@Test
	@DisplayName("사용자에게 온 친구 요청 목록을 성공적으로 조회한다")
	void getReceivedRequests_ShouldReturnRequestsList() {
		// Given
		List<FriendResponse> mockReceivedRequests = Arrays.asList(
			new FriendResponse(4L, "요청자A", "requesterA@example.com", "imgA.png"),
			new FriendResponse(5L, "요청자B", "requesterB@example.com", "imgB.png")
		);
		when(friendshipRepository.getReceivedRequestsOf(USER_ID)).thenReturn(mockReceivedRequests);

		// When
		GetFriendshipListResponse response = friendshipService.getReceivedRequests(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(2);
		assertThat(response.friends().get(0).nickname()).isEqualTo("요청자A");
		assertThat(response.friends().get(1).email()).isEqualTo("requesterB@example.com");
	}

	@Test
	@DisplayName("받은 친구 요청이 없을 때 빈 목록을 반환한다")
	void getReceivedRequests_ShouldReturnEmptyList_WhenNoRequests() {
		// Given
		when(friendshipRepository.getReceivedRequestsOf(USER_ID)).thenReturn(Collections.emptyList());

		// When
		GetFriendshipListResponse response = friendshipService.getReceivedRequests(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}

	@Test
	@DisplayName("사용자가 보낸 친구 요청 목록을 성공적으로 조회한다")
	void getSentRequests_ShouldReturnRequestsList() {
		// Given
		List<FriendResponse> mockSentRequests = Arrays.asList(
			new FriendResponse(6L, "받는이A", "receiverA@example.com", "imgA.png"),
			new FriendResponse(7L, "받는이B", "receiverB@example.com", "imgB.png")
		);
		when(friendshipRepository.getSentRequestsOf(USER_ID)).thenReturn(mockSentRequests);

		// When
		GetFriendshipListResponse response = friendshipService.getSentRequests(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(2);
		assertThat(response.friends().get(0).nickname()).isEqualTo("받는이A");
		assertThat(response.friends().get(1).email()).isEqualTo("receiverB@example.com");
	}

	@Test
	@DisplayName("보낸 친구 요청이 없을 때 빈 목록을 반환한다")
	void getSentRequests_ShouldReturnEmptyList_WhenNoRequests() {
		// Given
		when(friendshipRepository.getSentRequestsOf(USER_ID)).thenReturn(Collections.emptyList());

		// When
		GetFriendshipListResponse response = friendshipService.getSentRequests(USER_ID);

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}
}
