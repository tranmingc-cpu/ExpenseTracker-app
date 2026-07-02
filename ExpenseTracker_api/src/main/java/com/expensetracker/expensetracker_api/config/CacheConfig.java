package com.expensetracker.expensetracker_api.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Bật Spring Cache với Caffeine làm in-memory provider.
 * Cấu hình TTL và maxSize được đặt trong application.yaml (spring.cache.caffeine.spec).
 */
@Configuration
@EnableCaching
public class CacheConfig {
    // Cấu hình hoàn toàn qua application.yaml:
    // spring.cache.type=caffeine
    // spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=60m
}
