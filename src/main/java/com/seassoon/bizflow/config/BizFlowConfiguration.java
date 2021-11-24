package com.seassoon.bizflow.config;

import cn.hutool.core.lang.Assert;
import com.seassoon.bizflow.core.component.mq.RedisSanctuary;
import com.seassoon.bizflow.core.component.mq.Sanctuary;
import com.seassoon.bizflow.core.component.ocr.OcrProcessor;
import com.seassoon.bizflow.core.component.ocr.PaddleOcrProcessor;
import com.seassoon.bizflow.core.component.ocr.RedisOcrProcessor;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@SpringBootConfiguration
public class BizFlowConfiguration {

    @Autowired
    private BizFlowProperties properties;

    @Bean
    @ConditionalOnProperty(prefix = "biz-flow.minio", name = "enabled", havingValue = "true", matchIfMissing = true)
    public MinioClient minioClient() {
        BizFlowProperties.Minio minio = properties.getMinio();
        return MinioClient.builder()
                .endpoint(minio.getEndpoint(), minio.getPort(), false)
                .credentials(minio.getAccessKey(), minio.getSecretKey()).build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "biz-flow", name = "ocr-type", havingValue = "paddle")
    public OcrProcessor paddleOcrProcessor(RestTemplate restTemplate) {
        return new PaddleOcrProcessor(restTemplate, properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "biz-flow", name = "ocr-type", havingValue = "redis")
    public OcrProcessor redisOcrProcessor(Map<Integer, StringRedisTemplate> databaseRedisTemplate) {

        // 获取最后一个database作为OCR用
        StringRedisTemplate redisTemplate = databaseRedisTemplate.get(15);
        Assert.notNull(redisTemplate, "OCR type is redis, but no RedisTemplate created.");

        // 创建OcrProcessor
        RedisOcrProcessor processor = new RedisOcrProcessor();
        processor.setRedisTemplate(redisTemplate);
        processor.setRequestQueue(properties.getRedis().getQueue().get(BizFlowProperties.Redis.Queue.OCR_REQUEST));
        processor.setResponseQueue(properties.getRedis().getQueue().get(BizFlowProperties.Redis.Queue.OCR_RESPONSE));
        return processor;
    }

    // ---------- Sanctuary ----------
    @Bean
    public Sanctuary rdsSanctuary(Map<Integer, StringRedisTemplate> databaseRedisTemplate) {
        RedisSanctuary sanctuary = new RedisSanctuary();
        sanctuary.setDatabaseRedisTemplate(databaseRedisTemplate);
        sanctuary.setTimeout(properties.getRedis().getTimeout());
        return sanctuary;
    }
}
