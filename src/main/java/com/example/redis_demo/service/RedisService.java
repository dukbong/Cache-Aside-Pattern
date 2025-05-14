package com.example.redis_demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.redis_demo.request.FoodMenuRequest;
import com.example.redis_demo.response.FoodMenuResponse;
import com.example.redis_demo.util.CacheAside;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RedisService {

	private final RedisDatabaseService redisDatabaseService;
	
	public Long create(FoodMenuRequest foodMenuRequest) {
		Long saveId = redisDatabaseService.save(foodMenuRequest);
		return saveId;
	}
	
	@CacheAside(prefix = "foodMenu:", ttl = 500)
	public FoodMenuResponse findById(Long foodId) {
		return redisDatabaseService.findById(foodId);
	}

	@CacheAside(prefix = "foodMenu:price:greater:", ttl = 500)
	public List<FoodMenuResponse> findByPriceGreaterThan(Long price) {
		return redisDatabaseService.findByPriceGreaterThan(price);
	}

	
}
