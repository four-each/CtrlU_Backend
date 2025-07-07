package org.example.ctrlu.domain.friendship.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendshipScheduler {
	private final FriendshipService friendshipService;

	@Scheduled(cron = "0 0 0 * * *")
	public void deleteExpiredRejections() {
		log.info("친구 거절 내역 삭제 시작");
		Integer deleted = friendshipService.deleteExpiredRejections();
		log.info("친구 거절 내역 {}건 삭제 완료", deleted);
	}
}
