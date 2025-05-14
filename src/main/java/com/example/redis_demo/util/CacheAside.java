package com.example.redis_demo.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheAside {
	String prefix();

	/***
     * 캐시의 TTL(Time To Live) 값 (단위: 밀리초)
     * 기본값: 500ms
	 */
	long ttl() default 500;
}
