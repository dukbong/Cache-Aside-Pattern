package com.example.redis_demo.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FoodMenuResponse {
	
	private String name;
	
	private Long price;
	
	@Builder
	public FoodMenuResponse (String name, Long price) {
		this.name = name;
		this.price = price;
	}

}
