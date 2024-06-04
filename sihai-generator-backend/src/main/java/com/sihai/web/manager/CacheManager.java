package com.sihai.web.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 二级缓存操作
 */
@Component
public class CacheManager {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // 构建本地缓存
    Cache<String, Object> localCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    // 写入缓存
    public void put(String key, Object value) {
        localCache.put(key, value);
        // redis 缓存
        redisTemplate.opsForValue().set(key, value, 100, TimeUnit.MINUTES);
    }

    // 读取缓存
    public Object get(String key) {
        // 先从本地缓存中获取
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }

        // 如果本地缓存未命中，则从 redis 中获取
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            // 将 redis 缓存到本地
            localCache.put(key, value);
        }
        return value;
    }

    // 删除缓存
    public void delete(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }

}
