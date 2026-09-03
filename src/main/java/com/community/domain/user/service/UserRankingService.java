package com.community.domain.user.service;

import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import com.community.domain.user.dto.response.GetUserRankingResponse;
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

import static com.community.common.constant.AppConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRankingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordComment(Long userId) {
        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
            if (user == null) return;

            user.increaseCommentCount();
            userRepository.save(user);

            String weeklyKey = getWeeklyKey(UserRankType.COMMENT.name().toLowerCase());
            redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

            if (redisTemplate.getExpire(weeklyKey) == -1L) {
                redisTemplate.expire(weeklyKey, Duration.ofDays(USER_RANK_WEEKLY_DAYS));
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 댓글 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPost(Long userId) {
        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
            if (user == null) return;

            user.increasePostCount();
            userRepository.save(user);

            String weeklyKey = getWeeklyKey(UserRankType.POST.name().toLowerCase());
            redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

            if (redisTemplate.getExpire(weeklyKey) == -1L) {
                redisTemplate.expire(weeklyKey, Duration.ofDays(USER_RANK_WEEKLY_DAYS));
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 게시물 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordVisit(Long userId) {
        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
            if (user == null) return;

            String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            String dedupKey = USER_RANK_DEDUP_VISIT_PREFIX + userId + ":" + dateKey;
            Boolean isFirstVisit = redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "locked", Duration.ofHours(USER_RANK_DEDUP_VISIT_HOURS));

            if (Boolean.TRUE.equals(isFirstVisit)) {
                user.increaseVisitCount();
                userRepository.save(user);

                String weeklyKey = getWeeklyKey(UserRankType.VISIT.name().toLowerCase());
                redisTemplate.opsForZSet().incrementScore(weeklyKey, userId.toString(), 1);

                if (redisTemplate.getExpire(weeklyKey) == -1L) {
                    redisTemplate.expire(weeklyKey, Duration.ofDays(USER_RANK_WEEKLY_DAYS));
                }
            }
        } catch (Exception e) {
            log.error("[UserRankingService] 주간 방문 랭킹 집계 실패 - userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<GetUserRankingResponse> getWeeklyUserRanking(UserRankType type) {
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

                        return new GetUserRankingResponse(
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
        return USER_RANK_WEEKLY_PREFIX + ":" + type + ":" + year + ":" + weekNumber;
    }
}
