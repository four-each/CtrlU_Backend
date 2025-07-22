package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.domain.friendship.dto.response.FriendResponse;
import org.example.ctrlu.domain.friendship.dto.response.GetFriendshipListResponse;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GetFriendshipIntegrationTest {
	@Autowired
	private FriendshipService friendshipService;
	@Autowired
	private FriendshipRepository friendshipRepository;
	@Autowired
	private UserRepository userRepository;

	private User user;
	private User friend;
	private User requester;
	private User receiver;
	private User user2;
	static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;
	@Container
	public static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379);

	@DynamicPropertySource
	public static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
		registry.add("spring.datasource.username", mySQLContainer::getUsername);
		registry.add("spring.datasource.password", mySQLContainer::getPassword);
		registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);

		registry.add("spring.data.redis.host", redisContainer::getHost);
		registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
	}

	@BeforeEach
	void setUp() {
		friendshipRepository.deleteAll();
		userRepository.deleteAll();

		user = User.builder().nickname("유저").email("user@example.com").password("pass").image("img1.png").build();
		friend = User.builder().nickname("친구").email("friend@example.com").password("pass").image("img2.png").build();
		requester = User.builder().nickname("요청자").email("requester@example.com").password("pass").image("img3.png").build();
		receiver = User.builder().nickname("받는이").email("receiver@example.com").password("pass").image("img4.png").build();
		user2 = User.builder().nickname("관계없는이").email("stranger@example.com").password("pass").image("img5.png").build();

		user = userRepository.save(user);
		friend = userRepository.save(friend);
		requester = userRepository.save(requester);
		receiver = userRepository.save(receiver);
		user2 = userRepository.save(user2);

		Friendship request1 = Friendship.builder().fromUser(user).toUser(friend).build();
		request1.accept();
		friendshipRepository.save(request1);

		Friendship request2 = Friendship.builder().fromUser(requester).toUser(user).build();
		friendshipRepository.save(request2);

		Friendship request3 = Friendship.builder().fromUser(user).toUser(receiver).build();
		friendshipRepository.save(request3);

		Friendship request4 = Friendship.builder().fromUser(requester).toUser(receiver).build();
		request4.accept();
		friendshipRepository.save(request4);
	}

	@Test
	@DisplayName("사용자의 친구 목록을 최신순으로 조회한다")
	void getFriends_ShouldReturnFriendsList() {
		// When
		GetFriendshipListResponse response = friendshipService.getFriends(user.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(1);
		FriendResponse friendResponse = response.friends().get(0);
		assertThat(friendResponse.id()).isEqualTo(friend.getId());
		assertThat(friendResponse.nickname()).isEqualTo(friend.getNickname());
		assertThat(friendResponse.email()).isEqualTo(friend.getEmail());
	}

	@Test
	@DisplayName("친구가 없을 때 빈 친구 목록을 반환한다")
	void getFriends_ShouldReturnEmptyList_WhenNoFriends() {
		// When
		GetFriendshipListResponse response = friendshipService.getFriends(user2.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}

	@Test
	@DisplayName("사용자에게 온 친구 요청 목록을 최신순으로 조회한다")
	void getReceivedRequests_ShouldReturnRequestsList() {
		// When
		GetFriendshipListResponse response = friendshipService.getReceivedRequests(user.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(1);
		FriendResponse friendResponse = response.friends().get(0);
		assertThat(friendResponse.id()).isEqualTo(requester.getId());
		assertThat(friendResponse.nickname()).isEqualTo(requester.getNickname());
		assertThat(friendResponse.email()).isEqualTo(requester.getEmail());
	}

	@Test
	@DisplayName("받은 친구 요청이 없을 때 빈 목록을 반환한다")
	void getReceivedRequests_ShouldReturnEmptyList_WhenNoRequests() {
		// When
		GetFriendshipListResponse response = friendshipService.getReceivedRequests(user2.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}

	@Test
	@DisplayName("사용자가 보낸 친구 요청 목록을 최신순으로 조회한다")
	void getSentRequests_ShouldReturnRequestsList() {
		// When
		GetFriendshipListResponse response = friendshipService.getSentRequests(user.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).hasSize(1);
		FriendResponse friendResponse = response.friends().get(0);
		assertThat(friendResponse.id()).isEqualTo(receiver.getId());
		assertThat(friendResponse.nickname()).isEqualTo(receiver.getNickname());
		assertThat(friendResponse.email()).isEqualTo(receiver.getEmail());
	}

	@Test
	@DisplayName("보낸 친구 요청이 없을 때 빈 목록을 반환한다")
	void getSentRequests_ShouldReturnEmptyList_WhenNoRequests() {
		// When
		GetFriendshipListResponse response = friendshipService.getSentRequests(user2.getId());

		// Then
		assertThat(response).isNotNull();
		assertThat(response.friends()).isEmpty();
	}
}
