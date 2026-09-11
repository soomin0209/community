package com.community.domain.user.scheduler;

import com.community.domain.user.entity.User;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClearExpiredSuspensionsScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void clearExpiredSuspensions() {
        log.info("[ClearExpiredSuspensionsScheduler] 정지 기간 만료 사용자 정리 시작");
        int count = 0;

        List<User> userList = userRepository.findAllBySuspendedAtIsNotNull();
        for (User user : userList) {
            if (!user.isSuspended()) {
                user.unsuspend();
                count++;
            }
        }

        log.info("[ClearExpiredSuspensionsScheduler] 정지 기간 만료 사용자 정리 완료 - 사용자 수={}", count);
    }
}
