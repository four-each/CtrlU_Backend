package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.domain.friendship.dto.request.FriendshipRequest;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
import org.example.ctrlu.domain.friendship.repository.FriendshipRepository;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RequestFriendshipConcurrencyTest {
	@Autowired private FriendshipRedissonLockService friendshipRedissonLockService;
	@Autowired private UserRepository userRepository;
	@Autowired private FriendshipRepository friendshipRepository;
	@Autowired private PlatformTransactionManager transactionManager;
	private User userA;
	private User userB;
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
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			friendshipRepository.deleteAllInBatch();
			userRepository.deleteAllInBatch();

			userA = userRepository.save(User.builder()
				.nickname("loginUser")
				.email("usera@test.com")
				.password("password")
				.profileImageKey("image")
				.build()
			);
			userA.changeUserStatusToActive();

			userB = userRepository.save(User.builder()
				.nickname("targetUser")
				.email("userb@test.com")
				.password("password")
				.profileImageKey("image")
				.build()
			);
			userB.changeUserStatusToActive();

			// flush는 TransactionTemplate이 커밋될 때 자동으로 발생하지만, 명시적으로 호출해도 무방
			userRepository.flush();
		}); // 이 블록이 끝나면 트랜잭션이 커밋됩니다.
	}

	@AfterEach
	void tearDown() {
		friendshipRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
	}

	@Test
	@DisplayName("두 유저가 동시에 서로에게 친구 요청을 보낼 때, 하나의 요청만 성공하고 DB에는 하나의 관계만 저장된다.")
	void requestFriendship_ConcurrencyTest() throws InterruptedException {
		// given
		final int NUMBER_OF_REQUESTS_PER_USER = 50;
		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch readyLatch = new CountDownLatch(threadCount);
		CountDownLatch finishLatch = new CountDownLatch(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);
		AtomicInteger conflictCount = new AtomicInteger(0);
		AtomicInteger otherExceptionCount = new AtomicInteger(0);

		// when
		IntStream.range(0, NUMBER_OF_REQUESTS_PER_USER).forEach(i -> {
			// userA가 userB에게 친구 요청
			executorService.submit(() -> {
				try {
					readyLatch.countDown();
					readyLatch.await();

					friendshipRedissonLockService.requestFriendshipWithLock(userA.getId(), new FriendshipRequest(userB.getId()));
					successCount.incrementAndGet();
				} catch (FriendshipException e) {
					if (e.getExceptionStatus() == FriendshipErrorCode.FRIENDSHIP_REQUEST_CONFLICT) {
						conflictCount.incrementAndGet();
					} else {
						System.err.println("Unexpected FriendshipException: " + e.getExceptionStatus());
						otherExceptionCount.incrementAndGet();
					}
				} catch (Exception e) {
					System.err.println("Unexpected exception: " + e.getMessage());
					e.printStackTrace();
					otherExceptionCount.incrementAndGet();
				} finally {
					finishLatch.countDown();
				}
			});

			// userB가 userA에게 친구 요청
			executorService.submit(() -> {
				try {
					readyLatch.countDown();
					readyLatch.await();

					friendshipRedissonLockService.requestFriendshipWithLock(userB.getId(), new FriendshipRequest(userA.getId()));
					successCount.incrementAndGet();
				} catch (FriendshipException e) {
					if (e.getExceptionStatus() == FriendshipErrorCode.FRIENDSHIP_REQUEST_CONFLICT) {
						conflictCount.incrementAndGet();
					} else {
						System.err.println("Unexpected FriendshipException: " + e.getExceptionStatus());
						otherExceptionCount.incrementAndGet();
					}
				} catch (Exception e) {
					System.err.println("Unexpected exception: " + e.getMessage());
					e.printStackTrace();
					otherExceptionCount.incrementAndGet();
				} finally {
					finishLatch.countDown();
				}
			});
		});

		readyLatch.countDown();
		finishLatch.await(10, TimeUnit.SECONDS);
		executorService.shutdown();
		executorService.awaitTermination(10, TimeUnit.SECONDS);

		// then
		System.out.println("Success Count: " + successCount.get());
		System.out.println("Conflict Count: " + conflictCount.get());
		System.out.println("Other Exception Count: " + otherExceptionCount.get());

		long friendshipCount = friendshipRepository.count();
		assertThat(friendshipCount).isEqualTo(1);

		Friendship savedFriendship = friendshipRepository.findAll().get(0);
		assertThat(savedFriendship.getStatus()).isEqualTo(FriendshipStatus.PENDING);

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(conflictCount.get() + otherExceptionCount.get()).isEqualTo(NUMBER_OF_REQUESTS_PER_USER*2-1);
	}
}
