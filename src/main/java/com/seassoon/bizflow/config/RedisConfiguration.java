package com.seassoon.bizflow.config;

import cn.hutool.core.lang.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置信息，支持多databases配置
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@SpringBootConfiguration
public class RedisConfiguration {

    @Autowired
    private BizFlowProperties properties;

    /**
     * Redis多database连接信息
     *
     * @return key为database，value为{@link StringRedisTemplate}
     */
    @Bean
    public Map<Integer, StringRedisTemplate> databaseRedisTemplate() {
        Map<Integer, StringRedisTemplate> map = new HashMap<>();

        // 读取配置
        BizFlowProperties.Redis redis = properties.getRedis();

        // check configurations
        Assert.notBlank(redis.getHost(), "Redis host cannot be null or blank.");
        Assert.notNull(redis.getPort(), "Redis port cannot be null.");
        Assert.notBlank(redis.getPassword(), "Redis password cannot be null or blank.");

        // 分别为每一个redis database创建连接
        redis.getDatabases().forEach(database -> {
            // 创建ConnectionFactory
            LettuceConnectionFactory factory = new LettuceConnectionFactory(createRedisConfiguration(redis, database));
            map.put(database, new StringRedisTemplate(factory));
        });

        // 创建最后一个database，作为OCR type为redis的时候用
        if (properties.getOcrType() == BizFlowProperties.OcrType.REDIS) {
            int lastRedisDatabase = 15;
            LettuceConnectionFactory factory = new LettuceConnectionFactory(createRedisConfiguration(redis, lastRedisDatabase));
            map.put(lastRedisDatabase, new StringRedisTemplate(factory));
        }

        RedisSerializer<String> serializer = new StringRedisSerializer();
        return map;
    }

    private RedisStandaloneConfiguration createRedisConfiguration(BizFlowProperties.Redis redisProp, Integer database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProp.getHost());
        config.setPassword(redisProp.getPassword());
        config.setPassword(RedisPassword.of(redisProp.getPassword()));
        config.setDatabase(database);
        return config;
    }
}
