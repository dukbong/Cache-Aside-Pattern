package com.example.redis_demo.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.redis_demo.request.FoodMenuRequest;
import com.example.redis_demo.response.FoodMenuResponse;
import com.example.redis_demo.util.CacheAside;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

	private final ObjectMapper objectMapper;
	private final RedisDatabaseService redisDatabaseService;
	private final RedisTemplate<String, String> stringRedisTemplate;
	
	private static final String FOOD_MENU_KEY_PREFIX = "foodMenu:";
	private static final Duration CACHE_TTL = Duration.ofSeconds(5);
	
	// write-thourgh ( Json )
	public Long create(FoodMenuRequest foodMenuRequest) {
		Long saveId = redisDatabaseService.save(foodMenuRequest);
		String key = generateKey(saveId);

		try {
			String jsonValue = objectMapper.writeValueAsString(foodMenuRequest);
			stringRedisTemplate.opsForValue().set(key, jsonValue, CACHE_TTL);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Redis 저장 중 JSON 직렬화 오류", e);
		}

		return saveId;
	}


	// cache aside ( json )
	@CacheAside(prefix = "foodMenu:", ttl = 500)
	public FoodMenuResponse findById(Long foodId) {
		return redisDatabaseService.findById(foodId);
	}

	private String generateKey(Long id) {
		return FOOD_MENU_KEY_PREFIX + id;
	}
	
}
