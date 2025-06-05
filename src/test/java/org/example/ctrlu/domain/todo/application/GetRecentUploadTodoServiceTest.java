package org.example.ctrlu.domain.todo.application;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.config.TestRedisConfig;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.repository.FriendShipRepository;
import org.example.ctrlu.domain.todo.dto.response.GetRecentUploadTodoResponse;
import org.example.ctrlu.domain.todo.entity.Todo;
import org.example.ctrlu.domain.todo.entity.TodoStatus;
import org.example.ctrlu.domain.todo.exception.TodoErrorCode;
import org.example.ctrlu.domain.todo.exception.TodoException;
import org.example.ctrlu.domain.todo.repository.TodoRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.google.common.reflect.Reflection;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Testcontainers
@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class GetRecentUploadTodoServiceTest {

    /**
     * 24시간 내에 등록된 할 일 상세 조회 테스트 시나리오
     * - 초기 요청 + 본 이력이 없는 경우
     * - 초기 요청 + 본 이력이 있지만 유효하지 않은 경우
     * - 초기 요청 + 본 이력이 유효하고 마지막 커서인 경우
     * - 초기 요청 + 본 이력이 유효하고 마지막 커서가 아닌 경우
     * - 다음 커서 요청 + 본 이력이 없는 경우
     * - 다음 커서 요청 + 본 이력이 있지만 갱신할 필요가 없는 경우
     * - 다음 커서 요청 + 본 이력이 유효하고 갱신해야하는 경우
     * - 유효하지 않은 커서 요청인 경우
     */

    @Autowired private TodoRepository todoRepository;
    @Autowired private TodoService todoService;
    @Autowired private UserRepository userRepository;
    @Autowired private FriendShipRepository friendShipRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    public static final LocalTime TODO_CHALLENGE_TIME = LocalTime.of(10, 30);
    public static final String TEST_IMAGE = "test-image.png";

    private long userId ;
    private long targetId;
    private long firstTodoId;
    private long secondTodoId;
    private long thirdTodoId;

    private String redisKey;

    private User user;
    private User target;
    private Todo firstTodo;
    private Todo secondTodo;
    private Todo thirdTodo;

    static final MySQLContainer<?> mySQLContainer = TestMySQLConfig.MYSQL_CONTAINER;
    static final GenericContainer<?> redisContainer = TestRedisConfig.REDIS_CONTAINER;

    @DynamicPropertySource
    public static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mySQLContainer::getDriverClassName);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getExposedPorts);
    }

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        friendShipRepository.deleteAll();
        userRepository.deleteAll();
        Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection().flushAll();

        user = User.builder().nickname("유저 닉네임").email("test@gmail.com").password("pass").build();
        target = User.builder().nickname("타겟 닉네임").email("target@gmail.com").password("pass").build();

        userId = userRepository.save(user).getId();
        targetId = userRepository.save(target).getId();
        Friendship friendShip = Friendship.builder().fromUser(user).toUser(target).build();
        friendShip.accept();
        friendShipRepository.save(friendShip);
        redisKey = "recentTodo:seen:" + userId;

        firstTodo = Todo.builder().title("첫 번째 할일").challengeTime(TODO_CHALLENGE_TIME).startImage(TEST_IMAGE).user(target).build();
        secondTodo = Todo.builder().title("두 번째 할일").challengeTime(TODO_CHALLENGE_TIME).startImage(TEST_IMAGE).user(target).build();
        thirdTodo = Todo.builder().title("세 번째 할일").challengeTime(TODO_CHALLENGE_TIME).startImage(TEST_IMAGE).user(target).build();

        firstTodoId = todoRepository.save(firstTodo).getId();
        secondTodoId = todoRepository.save(secondTodo).getId();
        thirdTodoId = todoRepository.save(thirdTodo).getId();
    }

    @Test
    @DisplayName("초기 요청 - 본 이력이 없는 경우 - firstTodo 반환 + Redis 갱신")
    void getInitialTodo_noSeenHistory_returnsFirstTodo() {
        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, 0L);

        // then
        assertThat(response.nextId()).isEqualTo(secondTodoId);
        assertThat(response.prevId()).isEqualTo(null);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(firstTodoId));
    }

    @Test
    @DisplayName("초기 요청 - 본 이력 있지만 유효하지 않음 - firstTodo 반환 + Redis 갱신")
    void getInitialTodo_invalidSeenHistory_returnsFirstTodo() {
        // given
        redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(999L));

        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, 0L);

        System.out.println("테스트" + response.totalCount());
        // then
        assertThat(response.nextId()).isEqualTo(secondTodoId);
        assertThat(response.prevId()).isEqualTo(null);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(firstTodoId));
    }

    @Test
    @DisplayName("초기 요청 - 본 이력이 이미 가장 최신 - firstTodo 반환 + Redis 변동 없음")
    void getInitialTodo_validSeenHistory_isLastTodo_returnsFirstAgain() {
        // given
        redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(thirdTodoId));

        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, 0);

        // then
        assertThat(response.nextId()).isEqualTo(secondTodoId);
        assertThat(response.prevId()).isEqualTo(null);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(thirdTodoId));
    }

    @Test
    @DisplayName("초기 요청 - 본 이력이 가장 최신 게 아닌 경우 - 그 다음 할 일 반환 + Redis 갱신")
    void getInitialTodo_validSeenHistory_notLastTodo_returnsNextTodo() {
        // given
        redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(firstTodoId));

        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, 0);

        // then
        assertThat(response.nextId()).isEqualTo(thirdTodoId);
        assertThat(response.prevId()).isEqualTo(firstTodoId);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(secondTodoId));
    }

    @Test
    @DisplayName("다음 커서 요청 - 본 이력이 없는 경우 - 해당 커서 반환 + Redis 갱신")
    void getNextTodo_noSeenHistory_returnsNowIdTodoAndUpdatesRedis() {
        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, firstTodoId);

        // then
        assertThat(response.nextId()).isEqualTo(secondTodoId);
        assertThat(response.prevId()).isEqualTo(null);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(firstTodoId));
    }

    @Test
    @DisplayName("다음 커서 요청 - 본 이력이 이미 가장 최신 - Redis 변동 없음")
    void getNextTodo_seenHistoryNotUpdateNeeded_returnsNowIdTodo() {
        // given
        redisTemplate.opsForHash().put(redisKey, String.valueOf(targetId), String.valueOf(thirdTodoId));

        // when
        GetRecentUploadTodoResponse response = todoService.getRecentUploadTodo(userId, targetId, firstTodoId);

        // then
        assertThat(response.nextId()).isEqualTo(secondTodoId);
        assertThat(response.prevId()).isEqualTo(null);
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(redisTemplate.opsForHash().get(redisKey, String.valueOf(targetId))).isEqualTo(String.valueOf(thirdTodoId));
    }

    @Test
    @DisplayName("유효하지 않은 커서 요청인 경우 예외 발생")
    void getNextTodo_invalidNowId_throwsException() {
        // given
        long invalidTodoId = 999L;

        // when & then
        assertThatThrownBy(() -> todoService.getRecentUploadTodo(userId, targetId, invalidTodoId))
                .isInstanceOf(TodoException.class)
                .hasMessageContaining(TodoErrorCode.NOT_FOUND_TODO.getMessage());
    }
}
