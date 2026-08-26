package com.community.domain.user.service;

import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.user.dto.response.UserGetRanking5Response;
import com.community.domain.user.entity.User;
import com.community.domain.user.enums.UserRankType;
import com.community.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

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

    @Transactional(readOnly = true)
    public List<UserGetRanking5Response> getWeeklyUserRanking(UserRankType type) {
        try {
            String weeklyKey = getWeeklyKey(type.name().toLowerCase());
            Set<ZSetOperations.TypedTuple<Object>> result = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(weeklyKey, 0, 4);

            if (result == null || result.isEmpty()) return List.of();

            return result.stream()
                    .map(tuple -> {
                        Long userId = Long.parseLong((String) tuple.getValue());
                        User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);

                        if (user == null) {
                            return null;
                        }

                        return new UserGetRanking5Response(
                                user.getId(),
                                user.getNickname(),
                                tuple.getScore() != null ? tuple.getScore().longValue() : 0L
                        );
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 사용자 랭킹 조회 실패 - type={}, msg={}", type, e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }
    }

    private String getWeeklyKey(String type) {
        LocalDate now = LocalDate.now();
        int year = now.get(WeekFields.ISO.weekBasedYear());
        int weekNumber = now.get(WeekFields.ISO.weekOfWeekBasedYear());
        return WEEKLY_KEY_PREFIX + ":" + type + ":" + year + ":" + weekNumber;
    }
}
