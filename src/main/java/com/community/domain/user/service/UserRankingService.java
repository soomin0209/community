package com.community.domain.user.service;

import com.community.domain.user.enums.UserRankType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRankingService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String WEEKLY_KEY_PREFIX = "user:rank:week";
    private static final String DEDUP_KEY_PREFIX = "user:rank:dedup:visit:";
    private static final long WEEKLY_KEY_TTL = 8;   // 8일
    private static final long DEDUP_KEY_TTL = 26;   // 26시간

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordComment(Long userId) {
        try {
            String weeklyKey = getWeeklyKey(UserRankType.COMMENT.name().toLowerCase());
            redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

            if (redisTemplate.getExpire(weeklyKey) == -1L) {
                redisTemplate.expire(weeklyKey, Duration.ofDays(WEEKLY_KEY_TTL));
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 댓글 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPost(Long userId) {
        try {
            String weeklyKey = getWeeklyKey(UserRankType.POST.name().toLowerCase());
            redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

            if (redisTemplate.getExpire(weeklyKey) == -1L) {
                redisTemplate.expire(weeklyKey, Duration.ofDays(WEEKLY_KEY_TTL));
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 게시물 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVisit(Long userId) {
        try {
            String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String dedupKey = DEDUP_KEY_PREFIX + userId + ":" + dateKey;
            Boolean isFirstVisit = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "locked", Duration.ofHours(DEDUP_KEY_TTL));

            if (Boolean.TRUE.equals(isFirstVisit)) {
                String weeklyKey = getWeeklyKey(UserRankType.VISIT.name().toLowerCase());
                redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

                if (redisTemplate.getExpire(weeklyKey) == -1L) {
                    redisTemplate.expire(weeklyKey, Duration.ofDays(WEEKLY_KEY_TTL));
                }
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 방문 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    private String getWeeklyKey(String type) {
        LocalDate now = LocalDate.now();
        int year = now.get(WeekFields.ISO.weekBasedYear());
        int weekNumber = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        return WEEKLY_KEY_PREFIX + ":" + type + ":" + year + ":" + weekNumber;
    }
}
