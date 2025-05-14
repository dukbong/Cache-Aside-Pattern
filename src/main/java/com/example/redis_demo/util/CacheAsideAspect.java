package com.example.redis_demo.util;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAsideAspect {

    private final RedisTemplate<String, String> stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.example.redis_demo.util.CacheAside)")
    public Object cacheAsideMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        CacheAside cacheAside = method.getAnnotation(CacheAside.class);

        String prefix = cacheAside.prefix();
        long ttlMillisecond = cacheAside.ttl();

        if (!StringUtils.hasText(prefix)) {
            return joinPoint.proceed();
        }

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("조회할 Id 정보가 없습니다.");
        }

        Object arg = args[0];

        if (arg instanceof Long id) {
            return handleSingleIdRetrieval(joinPoint, method, prefix, ttlMillisecond, id);
        }

        if (arg instanceof List<?> idList && !idList.isEmpty() && idList.get(0) instanceof Long) {
            return handleMultiIdRetrieval(joinPoint, method, prefix, ttlMillisecond, (List<Long>) idList);
        }

        if (arg instanceof Set<?> idSet && !idSet.isEmpty() && idSet.iterator().next() instanceof Long) {
            return handleMultiIdRetrieval(joinPoint, method, prefix, ttlMillisecond, new ArrayList<>((Set<Long>) idSet));
        }

        throw new IllegalArgumentException("조회할 키의 타입이 Long, List<Long>, Set<Long> 중 하나여야 합니다.");
    }

    private Object handleSingleIdRetrieval(ProceedingJoinPoint joinPoint, Method method, String prefix, long ttlMillisecond, Long id) throws Throwable {
        String key = generatorKey(prefix, id);
        String cachedJson = getCachedData(key);
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();

        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, returnType);
            } catch (Exception e) {
                log.warn("Redis 캐시 역직렬화 실패 (단일 ID): {} - {}", key, e.getMessage());
                return databaseSearchAndCache(joinPoint, prefix, ttlMillisecond, id, returnType);
            }
        }

        return databaseSearchAndCache(joinPoint, prefix, ttlMillisecond, id, returnType);
    }

    private Object databaseSearchAndCache(ProceedingJoinPoint joinPoint, String prefix, long ttlMillisecond, Long id, Class<?> returnType) throws Throwable {
        Object dbResult = joinPoint.proceed();
        if (dbResult != null) {
            String key = generatorKey(prefix, id);
            cacheResult(key, dbResult, ttlMillisecond);
        }
        return dbResult;
    }

    private <T> Object handleMultiIdRetrieval(ProceedingJoinPoint joinPoint, Method method, String prefix, long ttlMillisecond, List<Long> ids) throws Throwable {
        List<String> keys = ids.stream().map(id -> generatorKey(prefix, id)).toList();
        Map<Long, Object> cachedResults = new LinkedHashMap<>();
        List<Long> missingIds = new ArrayList<>();
        List<String> cachedJsons = multiGetCachedData(keys);
        Class<T> elementType = resolveListElementType(method);

        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            String json = cachedJsons.get(i);

            if (StringUtils.hasText(json)) {
                try {
                    cachedResults.put(id, objectMapper.readValue(json, elementType));
                } catch (Exception e) {
                    log.warn("Redis 캐시 역직렬화 실패 (복수 ID): {} - {}", generatorKey(prefix, id), e.getMessage());
                    missingIds.add(id);
                }
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            Object[] newArgs = new Object[]{missingIds};
            Object dbResult = joinPoint.proceed(newArgs);

            if (dbResult instanceof List<?> dbList) {
                for (Object item : dbList) {
                    if (item != null) {
                        try {
                            Long itemId = extractId(item);
                            cacheResult(generatorKey(prefix, itemId), item, ttlMillisecond);
                            cachedResults.put(itemId, item);
                        } catch (Exception e) {
                            log.warn("ID 추출 또는 직렬화 실패 (복수 ID): {} - {}", item, e.getMessage());
                        }
                    }
                }
                return new ArrayList<>(cachedResults.values());
            }
        }

        return new ArrayList<>(cachedResults.values());
    }

    private String getCachedData(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    // 복수 조회 시 사이즈에 맞게 null 반환하여 다음 로직에서 처리하도록 
    private List<String> multiGetCachedData(List<String> keys) {
        try {
            return stringRedisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            log.warn("Redis multiGet 실패: {}", e.getMessage());
            return Collections.nCopies(keys.size(), null);
        }
    }

    private void cacheResult(String key, Object result, long ttlMillisecond) {
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, json, Duration.ofMillis(ttlMillisecond));
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패 ({}): {}", key, e.getMessage());
        }
    }

    private <T> Class<T> resolveListElementType(Method method) {
        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType pt) {
            Type actualTypeArgument = pt.getActualTypeArguments()[0];
            if (actualTypeArgument instanceof Class<?>) {
                return (Class<T>) actualTypeArgument;
            }
        }
        return (Class<T>) Object.class;
    }

    private Long extractId(Object obj) throws IllegalStateException {
        try {
            Method getIdMethod = obj.getClass().getMethod("getId");
            Object idObject = getIdMethod.invoke(obj);
            if (idObject instanceof Long) {
                return (Long) idObject;
            } else {
                throw new IllegalStateException("getId() 메서드의 반환 타입은 Long이어야 합니다.");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("해당 객체에 getId() 메서드가 존재하지 않습니다.");
        } catch (Exception e) {
            throw new IllegalStateException("getId() 메서드 호출 중 오류 발생: " + e.getMessage());
        }
    }

    private String generatorKey(String prefix, Long id) {
        return prefix + id;
    }
}