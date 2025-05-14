package com.example.redis_demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.redis_demo.entity.FoodMenu;

public interface FoodMenuRepository extends JpaRepository<FoodMenu, Long> {
	
	Optional<FoodMenu> findById(Long id);

    List<FoodMenu> findByPriceGreaterThan(Long price);

    List<FoodMenu> findByPriceLessThan(Long price);

    List<FoodMenu> findByPriceBetween(Long minPrice, Long maxPrice);

}
