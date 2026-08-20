package com.community.common.aop;

import com.community.common.annotation.Idempotent;
import com.community.common.exception.CommonExceptionEnum;
import com.community.common.exception.ServiceErrorException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedisTemplate<String, Object> redisTemplate;

    @Around("@annotation(idempotent)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String key = request.getHeader((idempotent.headerName()));

        if (!StringUtils.hasText(key)) {
            throw new ServiceErrorException(CommonExceptionEnum.MISSING_IDEMPOTENCY_KEY);
        }

        String redisKey = "idempotency:" + key;

        Boolean isFirstRequest;
        try {
            isFirstRequest = redisTemplate.opsForValue()
                    .setIfAbsent(redisKey, "PROCESSING", Duration.ofSeconds(idempotent.expireTime()));
        } catch (Exception e) {
            log.error("[IdempotencyAspect] Idempotency-Key 중복 체크 실패 - key={}, msg={}", redisKey, e.getMessage());
            throw new ServiceErrorException(CommonExceptionEnum.REDIS_CONNECTION_ERROR);
        }

        if (Boolean.FALSE.equals(isFirstRequest)) {
            throw new ServiceErrorException(CommonExceptionEnum.DUPLICATE_REQUEST);
        }

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            try {
                redisTemplate.delete(redisKey);
            } catch (Exception re) {
                log.warn("[IdempotencyAspect] Idempotency-Key 삭제 실패 - key={}, msg={}", redisKey, re.getMessage());
                // delete 실패해도 TTL로 자동 만료
            }
            throw e;
        }
    }
}
