package com.community.domain.user.scheduler;

import com.community.domain.user.dto.UserCountProjection;
import com.community.domain.user.entity.User;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCountSyncScheduler {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncUserCount() {
        log.info("[UserCountSyncScheduler] 정합성 보정 시작");
        int postSyncCount = 0;
        int commentSyncCount = 0;

        List<UserCountProjection> userCounts = userRepository.findUserCounts();
        Map<Long, User> userMap = userRepository.findAllByDeletedAtIsNull().stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        for (UserCountProjection projection : userCounts) {
            User user = userMap.get(projection.userId());
            if (user == null) continue;

            if (!user.getPostCount().equals(projection.postCount())) {
                log.warn("[UserCountSyncScheduler] 총 게시물 수 불일치 - userId={}, 현재={}, 실제={}",
                        user.getId(), user.getPostCount(), projection.postCount());
                user.setPostCount(projection.postCount());
                postSyncCount++;
            }

            if (!user.getCommentCount().equals(projection.commentCount())) {
                log.warn("[UserCountSyncScheduler] 총 댓글 수 불일치 - userId={}, 현재={}, 실제={}",
                        user.getId(), user.getCommentCount(), projection.commentCount());
                user.setCommentCount(projection.commentCount());
                commentSyncCount++;
            }
        }

        log.info("[UserCountSyncScheduler] 정합성 보정 완료 - 게시물 {}건, 댓글 {}건", postSyncCount, commentSyncCount);
    }
}
