package com.community.domain.post.service;

import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.post.dto.response.PostGetBest5Response;
import com.community.domain.post.entity.Post;
import com.community.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final PostRepository postRepository;

    private static final String WEEKLY_KEY_PREFIX = "post:view:week";
    private static final String DEDUP_KEY_PREFIX = "post:view:dedup:";
    private static final long WEEKLY_KEY_TTL = 60 * 60 * 24 * 8;   // 8일
    private static final long DEDUP_KEY_TTL = 60 * 60 * 24;     // 1일

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long postId, String clientIp) {
        try {
            String dedupKey = DEDUP_KEY_PREFIX + postId + ":" + clientIp;
            Boolean isFirstView = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "locked", Duration.ofSeconds(DEDUP_KEY_TTL));

            if (Boolean.TRUE.equals(isFirstView)) {
                Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElse(null);
                if (post != null) {
                    post.incrementViewCount();
                    postRepository.save(post);
                }

                String weeklyKey = getWeeklyKey();
                redisTemplate.opsForZSet().incrementScore(weeklyKey, postId.toString(), 1);

                if (redisTemplate.getExpire(weeklyKey) == -1L) {
                    redisTemplate.expire(weeklyKey, Duration.ofSeconds(WEEKLY_KEY_TTL));
                }
            }
        } catch (Exception e) {
            log.error("[PostViewService] Redis ZSET 조회수 증가 실패 - postId={}, msg={}", postId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PostGetBest5Response> getWeeklyBestPosts() {
        try {
            String weeklyKey = getWeeklyKey();
            Set<ZSetOperations.TypedTuple<Object>> result = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(weeklyKey, 0, 4);

            if (result == null || result.isEmpty()) return List.of();

            return result.stream()
                    .map(tuple -> {
                        Long postId = Long.parseLong((String) tuple.getValue());
                        Post post = postRepository.findByIdAndDeletedAtIsNull(postId).orElse(null);

                        if (post == null) {
                            return null;
                        }

                        return new PostGetBest5Response(
                                post.getId(),
                                post.getTitle(),
                                tuple.getScore() != null ? tuple.getScore().longValue() : 0L,
                                post.getViewCount()
                        );
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("[PostViewService] 주간 인기 게시물 조회 실패 - msg={}", e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    private String getWeeklyKey() {
        LocalDate now = LocalDate.now();
        int weekNumber = now.get(WeekFields.of(Locale.KOREA).weekOfYear());
        return WEEKLY_KEY_PREFIX + now.getYear() + ":" + weekNumber;
    }
}
