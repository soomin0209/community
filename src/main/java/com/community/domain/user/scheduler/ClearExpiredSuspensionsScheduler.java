package com.community.domain.user.scheduler;

import com.community.domain.user.entity.User;
import com.community.domain.user.entity.UserSuspension;
import com.community.domain.user.repository.UserRepository;
import com.community.domain.user.repository.UserSuspensionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.community.common.constant.AppConstants.PERMANENT_SUSPENSION_DATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClearExpiredSuspensionsScheduler {

    private final UserRepository userRepository;
    private final UserSuspensionRepository userSuspensionRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void clearExpiredSuspensions() {
        log.info("[ClearExpiredSuspensionsScheduler] 정지 기간 만료 사용자 정리 시작");
        int count = 0;
        LocalDateTime now = LocalDateTime.now();

        List<User> userList = userRepository.findAllBySuspendedUntilIsNotNull();
        for (User user : userList) {
            if (user.getSuspendedUntil().equals(PERMANENT_SUSPENSION_DATE)) {
                continue;
            }

            if (user.getSuspendedUntil().isBefore(now)) {
                List<UserSuspension> suspensionList = userSuspensionRepository.findAllByUserIdAndUnsuspendedAtIsNull(user.getId());
                for (UserSuspension suspension : suspensionList) {
                    suspension.unsuspend(now, null);
                }
                user.resetSuspendedUntil();
                count++;
            }
        }

        log.info("[ClearExpiredSuspensionsScheduler] 정지 기간 만료 사용자 정리 완료 - 사용자 수={}", count);
    }
}
