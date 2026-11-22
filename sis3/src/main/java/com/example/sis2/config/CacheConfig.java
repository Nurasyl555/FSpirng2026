package com.example.sis2.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String BOOKS_CACHE = "books";
    public static final String BOOKS_LIST_CACHE = "booksList";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(BOOKS_CACHE, BOOKS_LIST_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats());
        return cacheManager;
    }
}
