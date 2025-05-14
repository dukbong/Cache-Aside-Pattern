package com.example.redis_demo.service;

import java.util.List;
import java.util.stream.Collector;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.redis_demo.entity.FoodMenu;
import com.example.redis_demo.repository.FoodMenuRepository;
import com.example.redis_demo.request.FoodMenuRequest;
import com.example.redis_demo.response.FoodMenuResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RedisDatabaseService {
	
	private final FoodMenuRepository foodMenuRepository;

	@Transactional
	public Long save(FoodMenuRequest foodMenuRequest) {
		FoodMenu foodMenu = FoodMenu.builder().name(foodMenuRequest.getName()).price(foodMenuRequest.getPrice()).build();
		return  foodMenuRepository.save(foodMenu).getId();
	}


	public FoodMenuResponse findById(Long foodId) {
		FoodMenu foodMenu = foodMenuRepository.findById(foodId).orElseThrow(() -> new IllegalArgumentException("메뉴를 찾을 수 없습니다."));
		return FoodMenuResponse.builder().name(foodMenu.getName()).price(foodMenu.getPrice()).build();
	}
	
    public List<FoodMenuResponse> findByPriceGreaterThan(Long price) {
        List<FoodMenu> menus = foodMenuRepository.findByPriceGreaterThan(price);
        return menus.stream().map(m -> FoodMenuResponse.builder().name(m.getName()).price(m.getPrice()).build()).toList();
    }

    public List<FoodMenuResponse> findByPriceLessThan(Long price) {
    	List<FoodMenu> menus = foodMenuRepository.findByPriceLessThan(price);
    	return menus.stream().map(m -> FoodMenuResponse.builder().name(m.getName()).price(m.getPrice()).build()).toList();
    }

    public List<FoodMenuResponse> findByPriceBetween(Long minPrice, Long maxPrice) {
    	List<FoodMenu> menus = foodMenuRepository.findByPriceBetween(minPrice, maxPrice);
    	return menus.stream().map(m -> FoodMenuResponse.builder().name(m.getName()).price(m.getPrice()).build()).toList();
    }
	

	
}
