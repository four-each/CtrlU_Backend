package org.example.ctrlu.domain.todo.application;


import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.config.TestRedisConfig;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.response.GetRecentUploadFriendsResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GetRecentUploadFriendsServiceTest {
    /**
     * 24시간 내에 할 일 등록된 친구 목록 조회 테스트 시나리오
     * - 친구가 없을 때 응답 성공
     * - 24시간 내에 내가 올린 할 일이 없을 때 응답 성공
     * - 24시간 내에 올린 친구가 없을 때 응답 성공
     * - 24시간 내에 올린 친구가 있고, Redis에 본 이력이 없는 경우 GREEN으로 응답 성공
     * - 24시간 내에 올린 친구가 있고, Redis에 본 이력이 있고, 새로 올린 경우 GREEN으로 응답 성공
     * - 24시간 내에 올린 친구가 있고, Redis에 본 이력이 있고, 그게 가장 최근인 경우 GRAY로 응답 성공
     * - createdAt 내림차순으로 정렬 성공
     * - 포기한 할 일은 조회에서 제외
     */

    @Autowired private TodoService todoService;
    @Autowired private TodoRepository todoRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendShipRepository friendShipRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private User me;
    private User friend;
    private static final String REDIS_KEY_PREFIX = "recentTodo:seen:";
    public static final String TODO_TITLE = "할 일 제목";
    public static final LocalTime TODO_CHALLENGE_TIME = LocalTime.of(10, 30);
    public static final String TEST_IMAGE = "test-image.png";

    static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;

    @Container // Testcontainers가 이 컨테이너의 생명주기를 관리하도록 합니다.
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
        todoRepository.deleteAll();
        friendShipRepository.deleteAll();
        userRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();

        me = userRepository.save(User.builder()
                .nickname("나")
                .email("me@ctrlu.com")
                .password("pw")
                .build());

        friend = userRepository.save(User.builder()
                .nickname("친구")
                .email("friend@ctrlu.com")
                .password("pw")
                .build());
    }

    @Test
    @DisplayName("24시간 내 생성한 할 일이 없을 때 응답")
    void getRecentUploadFriends_noMyTodo_returnsNull() {
        //given
        Todo todo = todoRepository.save(Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build());
        ReflectionTestUtils.setField(todo,"createdAt",LocalDateTime.now().minusHours(25));
        todoRepository.save(todo);

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.me().status()).isEqualTo(GetRecentUploadFriendsResponse.Status.NONE);
    }

    @Test
    @DisplayName("친구가 없을 때 응답")
    void getRecentUploadFriends_noFriend_returnsNull() {
        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).hasSize(0);
        assertThat(response.totalElementCount()).isEqualTo(0);
        assertThat(response.totalPageCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("본 이력이 없으면 GREEN 응답")
    void getRecentUploadFriends_notSeen_returnsGreen() {
        // given
        Friendship friendShip = Friendship.builder().fromUser(me).toUser(friend).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);
        todoRepository.save(Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build());

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).hasSize(1);
        assertThat(response.friends().get(0).status()).isEqualTo(GetRecentUploadFriendsResponse.Status.GREEN);
        assertThat(response.totalElementCount()).isEqualTo(1);
        assertThat(response.totalPageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("가장 최근 todoId와 본 todoId가 같으면 GRAY 응답")
    void getRecentUploadFriends_alreadySeen_returnsGray() {
        // given
        Friendship friendShip = Friendship.builder().fromUser(me).toUser(friend).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);

        Todo todo = todoRepository.save(Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build());

        String redisKey = REDIS_KEY_PREFIX + me.getId();
        redisTemplate.opsForHash().put(redisKey, String.valueOf(friend.getId()), String.valueOf(todo.getId()));

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).hasSize(1);
        assertThat(response.friends().get(0).status()).isEqualTo(GetRecentUploadFriendsResponse.Status.GRAY);
    }

    @Test
    @DisplayName("본 이력이 있고 최신보다 작으면 GREEN 응답")
    void getRecentUploadFriends_seenOldTodo_returnsGreen() {
        // given
        Friendship friendShip = Friendship.builder().fromUser(me).toUser(friend).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);

        Todo oldTodo = Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build();
        ReflectionTestUtils.setField(oldTodo,"createdAt",LocalDateTime.now().minusHours(10));
        todoRepository.save(oldTodo);

        Todo newTodo = Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build();
        ReflectionTestUtils.setField(newTodo,"createdAt",LocalDateTime.now().minusHours(1));
        todoRepository.save(newTodo);

        String redisKey = REDIS_KEY_PREFIX + me.getId();
        redisTemplate.opsForHash().put(redisKey, String.valueOf(friend.getId()), String.valueOf(oldTodo.getId()));

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).hasSize(1);
        assertThat(response.friends().get(0).status()).isEqualTo(GetRecentUploadFriendsResponse.Status.GREEN);
    }

    @Test
    @DisplayName("createdAt 내림차순으로 응답")
    void getRecentUploadFriends_desc_createdAt() {
        // given
        Friendship friendShip = Friendship.builder().fromUser(me).toUser(friend).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);

        User friend2 = userRepository.save(User.builder()
                .nickname("친구2")
                .email("friend2@ctrlu.com")
                .password("pw")
                .build());
        Friendship friendShip2 = Friendship.builder().fromUser(me).toUser(friend2).build();
        friendShip2.accept();
        friendShipRepository.save(friendShip2);

        Todo firstUploadFriendTodo = Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build();
        ReflectionTestUtils.setField(firstUploadFriendTodo,"createdAt",LocalDateTime.now().minusHours(10));
        todoRepository.save(firstUploadFriendTodo);

        Todo secondUploadFriendTodo = Todo.builder()
                .user(friend2)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build();
        ReflectionTestUtils.setField(secondUploadFriendTodo,"createdAt",LocalDateTime.now().minusHours(1));
        todoRepository.save(secondUploadFriendTodo);

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).hasSize(2);
        assertThat(response.friends().get(0).id()).isEqualTo(friend2.getId());
        assertThat(response.friends().get(1).id()).isEqualTo(friend.getId());
        assertThat(response.totalElementCount()).isEqualTo(2);
        assertThat(response.totalPageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("포기한 할 일은 조회되지 않음")
    void getRecentUploadFriends_excludeGivenUpTodo() {
        // given
        Friendship friendShip = Friendship.builder().fromUser(me).toUser(friend).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);

        // 포기한 할 일 등록
        Todo givenUpTodo = Todo.builder()
                .user(friend)
                .title(TODO_TITLE)
                .challengeTime(TODO_CHALLENGE_TIME)
                .startImage(TEST_IMAGE)
                .build();
        ReflectionTestUtils.setField(givenUpTodo, "createdAt", LocalDateTime.now().minusHours(1));
        givenUpTodo.giveUp();
        todoRepository.save(givenUpTodo);

        // when
        GetRecentUploadFriendsResponse response = todoService.getRecentUploadFriends(me.getId(), PageRequest.of(0, 10));

        // then
        assertThat(response.friends()).isEmpty();
        assertThat(response.totalElementCount()).isZero();
        assertThat(response.totalPageCount()).isZero();
    }

}
