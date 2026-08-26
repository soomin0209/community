package com.community.domain.user.scheduler;

import com.community.domain.comment.repository.CommentRepository;
import com.community.domain.post.repository.PostRepository;
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
public class UserCountSyncScheduler {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void syncUserCount() {
        log.info("[UserCountSyncScheduler] 정합성 보정 시작");
        int postSyncCount = 0;
        int commentSyncCount = 0;

        List<User> userList = userRepository.findAllByDeletedAtIsNull();

        for (User user : userList) {
            Long actualPostCount = postRepository.countByUserIdAndDeletedAtIsNull(user.getId());
            if (!user.getPostCount().equals(actualPostCount)) {
                log.warn("[UserCountSyncScheduler] 총 게시물 수 불일치 - userId={}, 현재={}, 실제={}",
                        user.getId(), user.getPostCount(), actualPostCount);
                user.setPostCount(actualPostCount);
                postSyncCount++;
            }

            Long actualCommentCount = commentRepository.countByUserIdAndDeletedAtIsNull(user.getId());
            if (!user.getCommentCount().equals(actualCommentCount)) {
                log.warn("[UserCountSyncScheduler] 총 댓글 수 불일치 - userId={}, 현재={}, 실제={}",
                        user.getId(), user.getCommentCount(), actualCommentCount);
                user.setCommentCount(actualCommentCount);
                commentSyncCount++;
            }
        }

        log.info("[UserCountSyncScheduler] 정합성 보정 완료 - 게시물 {}건, 댓글 {}건", postSyncCount, commentSyncCount);
    }
}
