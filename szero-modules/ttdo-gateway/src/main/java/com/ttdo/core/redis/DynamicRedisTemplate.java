package com.ttdo.core.redis;

import com.ttdo.core.redis.config.DynamicRedisTemplateFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class DynamicRedisTemplate<K, V> extends AbstractRoutingRedisTemplate<K, V> {
    private final DynamicRedisTemplateFactory<K, V> dynamicRedisTemplateFactory;

    public DynamicRedisTemplate(DynamicRedisTemplateFactory<K, V> dynamicRedisTemplateFactory) {
        this.dynamicRedisTemplateFactory = dynamicRedisTemplateFactory;
    }

    protected Object determineCurrentLookupKey() {
        return RedisDatabaseThreadLocal.get();
    }

    protected RedisTemplate<K, V> createRedisTemplateOnMissing(Object lookupKey) {
        return this.dynamicRedisTemplateFactory.createRedisTemplate((Integer)lookupKey);
    }
}
