package com.example.redis_demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.redis_demo.request.FoodMenuRequest;
import com.example.redis_demo.response.FoodMenuResponse;
import com.example.redis_demo.service.RedisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/redis")
public class RedisController {
	
	private final RedisService redisService;

	@PostMapping("/create")
	public ResponseEntity<Long> create(@RequestBody FoodMenuRequest foodMenuRequest) {
		Long result = redisService.create(foodMenuRequest);
		return ResponseEntity.ok().body(result);
	}
	
	@GetMapping("/find")
	public ResponseEntity<FoodMenuResponse> findById(@RequestParam(name = "foodId") Long foodId) {
		FoodMenuResponse result = redisService.findById(foodId);
		return ResponseEntity.ok().body(result);
	}
}
