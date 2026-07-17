package com.lion.benefito.domain.user.scheduler;

import com.lion.benefito.domain.user.entity.User;
import com.lion.benefito.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserCleanupScheduler {

    private final UserRepository userRepository;

    // 매 03:00
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupDeletedUsers() {
        log.info("Starting scheduled cleanup of deleted users...");

        // 한 달 전 날짜 계산
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

        // 한 달 이전에 soft deleted 회원 조회
        List<User> usersToDelete = userRepository.findByDeletedAtBefore(oneMonthAgo);

        if (usersToDelete.isEmpty()) {
            log.info("No users to delete.");
        }

        userRepository.deleteAll(usersToDelete);
    }
}
