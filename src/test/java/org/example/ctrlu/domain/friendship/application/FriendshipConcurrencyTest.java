package org.example.ctrlu.domain.friendship.application;

import static org.assertj.core.api.Assertions.*;
import static org.example.ctrlu.domain.friendship.exception.FriendshipErrorCode.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.ctrlu.config.TestMySQLConfig;
import org.example.ctrlu.domain.friendship.entity.Friendship;
import org.example.ctrlu.domain.friendship.entity.FriendshipStatus;
import org.example.ctrlu.domain.friendship.exception.FriendshipException;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FriendshipConcurrencyTest {
	@Autowired private FriendshipOptimisticLockService friendshipOptimisticLockService;
	@Autowired private UserRepository userRepository;
	@Autowired private FriendshipRepository friendshipRepository;
	@Autowired private PlatformTransactionManager transactionManager;
	private User userA;
	private User userB;
	private Friendship pendingFriendship;
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
		// 테스트마다 초기 데이터 클린업 (선택 사항, @Transactional이 롤백시키므로)
		friendshipRepository.deleteAll();
		userRepository.deleteAll();

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

		// PENDING 상태의 친구 요청 생성
		pendingFriendship = Friendship.builder()
			.fromUser(userA)
			.toUser(userB)
			.build();
		pendingFriendship = friendshipRepository.save(pendingFriendship);
	}

	@Test
	@DisplayName("낙관적 락: 수락 요청과 취소 요청이 동시에 발생 시, 충돌 처리 및 최종 상태 확인")
	void testAcceptAndCancelFriendshipConcurrency() throws InterruptedException {
		// Given: PENDING 상태의 친구 요청
		Long friendshipId = pendingFriendship.getId();

		CountDownLatch latch = new CountDownLatch(1); // 1이 되면 모든 대기 스레드 해제
		CountDownLatch finishLatch = new CountDownLatch(2); // 두 스레드 모두 완료될 때까지 기다림

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		AtomicInteger acceptSuccessCount = new AtomicInteger(0);
		AtomicInteger cancelSuccessCount = new AtomicInteger(0);
		AtomicInteger conflictCount = new AtomicInteger(0);
		AtomicInteger notFoundCount = new AtomicInteger(0);

		// When: 한 스레드는 수락, 다른 스레드는 취소 시도
		executorService.submit(() -> {
			latch.await();
			try {
				friendshipOptimisticLockService.cancelFriendship(userA.getId(), friendshipId);
				cancelSuccessCount.incrementAndGet();
			} catch (FriendshipException e) {
				if (e.getExceptionStatus() == FRIENDSHIP_CANCEL_CONFLICT) {
					conflictCount.incrementAndGet();
				} else if (e.getExceptionStatus() == NOT_FOUND_FRIENDSHIP) {
					notFoundCount.incrementAndGet();
				} else {
					System.err.println("Unexpected accept FriendshipException: " + e.getExceptionStatus());
				}
			} finally {
				finishLatch.countDown(); // 작업 완료 알림
			}
			return null;
		});

		executorService.submit(() -> {
			latch.await();
			try {
				friendshipOptimisticLockService.acceptFriendship(userB.getId(), friendshipId);
				acceptSuccessCount.incrementAndGet();
			} catch (FriendshipException e) {
				if (e.getExceptionStatus() == FRIENDSHIP_ACCEPT_CONFLICT) {
					conflictCount.incrementAndGet();
				} else if (e.getExceptionStatus() == NOT_FOUND_FRIENDSHIP) {
					notFoundCount.incrementAndGet();
				} else {
					System.err.println("Unexpected accept FriendshipException: " + e.getExceptionStatus());
				}
			} finally {
				finishLatch.countDown(); // 작업 완료 알림
			}
			return null;
		});

		latch.countDown();
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.SECONDS);

		// Then
		assertThat(acceptSuccessCount.get() + cancelSuccessCount.get()).isEqualTo(1);

		Friendship finalFriendship = friendshipRepository.findById(friendshipId).orElse(null);

		System.out.println("Accept Success: " + acceptSuccessCount.get());
		System.out.println("Cancel Success: " + cancelSuccessCount.get());
		System.out.println("Conflict Count: " + conflictCount.get());
		System.out.println("Not Found Count: " + notFoundCount.get());

		if (acceptSuccessCount.get() == 1) {
			System.out.println("수락");
			assertThat(finalFriendship).isNotNull();
			assertThat(finalFriendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
			assertThat(conflictCount.get()).isEqualTo(1);
		} else {
			System.out.println("취소");
			assertThat(finalFriendship).isNull();
			assertThat(conflictCount.get()).isEqualTo(1);
		}
	}

	@Test
	@DisplayName("낙관적 락: 거절 요청과 취소 요청이 동시에 발생 시, 충돌 처리 및 최종 상태 확인")
	void testRejectAndCancelFriendshipConcurrency() throws InterruptedException {
		// Given: PENDING 상태의 친구 요청
		Long friendshipId = pendingFriendship.getId();

		CountDownLatch latch = new CountDownLatch(1); // 1이 되면 모든 대기 스레드 해제
		CountDownLatch finishLatch = new CountDownLatch(2); // 두 스레드 모두 완료될 때까지 기다림

		ExecutorService executorService = Executors.newFixedThreadPool(2);
		AtomicInteger rejectSuccessCount = new AtomicInteger(0);
		AtomicInteger cancelSuccessCount = new AtomicInteger(0);
		AtomicInteger conflictCount = new AtomicInteger(0);
		AtomicInteger notFoundCount = new AtomicInteger(0);

		// When: 한 스레드는 거절, 다른 스레드는 취소 시도
		executorService.submit(() -> {
			latch.await();
			try {
				friendshipOptimisticLockService.cancelFriendship(userA.getId(), friendshipId);
				cancelSuccessCount.incrementAndGet();
			} catch (FriendshipException e) {
				if (e.getExceptionStatus() == FRIENDSHIP_CANCEL_CONFLICT) {
					conflictCount.incrementAndGet();
				} else if (e.getExceptionStatus() == NOT_FOUND_FRIENDSHIP) {
					notFoundCount.incrementAndGet();
				} else {
					System.err.println("Unexpected accept FriendshipException: " + e.getExceptionStatus());
				}
			} finally {
				finishLatch.countDown(); // 작업 완료 알림
			}
			return null;
		});

		executorService.submit(() -> {
			latch.await();
			try {
				friendshipOptimisticLockService.rejectFriendship(userB.getId(), friendshipId);
				rejectSuccessCount.incrementAndGet();
			} catch (FriendshipException e) {
				if (e.getExceptionStatus() == FRIENDSHIP_REJECT_CONFLICT) {
					conflictCount.incrementAndGet();
				} else if (e.getExceptionStatus() == NOT_FOUND_FRIENDSHIP) {
					notFoundCount.incrementAndGet();
				} else {
					System.err.println("Unexpected accept FriendshipException: " + e.getExceptionStatus());
				}
			} finally {
				finishLatch.countDown();
			}
			return null;
		});

		latch.countDown();
		executorService.shutdown();
		executorService.awaitTermination(1, TimeUnit.SECONDS);

		// Then
		assertThat(rejectSuccessCount.get() + cancelSuccessCount.get()).isEqualTo(1);

		Friendship finalFriendship = friendshipRepository.findById(friendshipId).orElse(null);

		System.out.println("Reject Success: " + rejectSuccessCount.get());
		System.out.println("Cancel Success: " + cancelSuccessCount.get());
		System.out.println("Conflict Count: " + conflictCount.get());
		System.out.println("Not Found Count: " + notFoundCount.get());

		if (rejectSuccessCount.get() == 1) {
			System.out.println("거절");
			assertThat(finalFriendship).isNotNull();
			assertThat(finalFriendship.getStatus()).isEqualTo(FriendshipStatus.REJECTED);
			assertThat(conflictCount.get()).isEqualTo(1);
		} else {
			System.out.println("취소");
			assertThat(finalFriendship).isNull();
			assertThat(conflictCount.get()).isEqualTo(1);
		}
	}
}
