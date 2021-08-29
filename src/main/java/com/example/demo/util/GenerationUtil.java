package com.example.demo.util;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component("GenerationUtil")
public final class GenerationUtil {
	public static String generateId() {
		return UUID.randomUUID().toString();
	}
	@Autowired
	private RedisTemplate<String, Integer> redisTemplateAutowired;
	private static RedisTemplate<String, Integer> redisTemplate;
	@PostConstruct
	private void init() {redisTemplate = this.redisTemplateAutowired;}
	public static String generateId(String s){
		//RedisCommands<String, String> com = redis.sync();
		String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
		String key = s +"_" + date;
		//com.incr(key);
		Integer num = redisTemplate.opsForValue().get(key) + 1;
		redisTemplate.opsForValue().increment(key);
		return date + "-" + String.format("%08d",num);
	}
}
