package com.seassoon.bizflow.core.component.ocr;

import cn.hutool.core.codec.Base64Encoder;
import com.google.common.collect.ImmutableMap;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * OCR识别的paddle实现，将图片内容转成base64并放入redis队列，然后监听redis响应队列结果
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class RedisOcrProcessor implements OcrProcessor {

    private static final Logger logger = LoggerFactory.getLogger(RedisOcrProcessor.class);

    private StringRedisTemplate redisTemplate;
    private String requestQueue;
    private String responseQueue;

    @Override
    public String process(Path file) {
        // 获取Redis队列Key

        String strOcrResponse = null;

        try {
            // 读取图片内容，转化为base64编码
            String strBase64 = Base64Encoder.encode(Files.readAllBytes(file));

            // 发送倒Redis队列，并监听结果
            Map<String, String> body = ImmutableMap.<String, String> builder()
                    .put("image_name", file.getFileName().toString())
                    .put("image_base64", strBase64)
                    .build();
            redisTemplate.opsForList().rightPush(requestQueue, Objects.requireNonNull(JSONUtils.writeValueAsString(body)));

            String strRespChannel = responseQueue + "-" + file.getFileName().toString();
            strOcrResponse = redisTemplate.opsForList().rightPop(strRespChannel, Duration.ofSeconds(60));
        } catch (IOException e) {
            logger.error("读取图片内容失败 - " + e.getMessage(), e);
        }

        return strOcrResponse;
    }

    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setRequestQueue(String requestQueue) {
        this.requestQueue = requestQueue;
    }

    public void setResponseQueue(String responseQueue) {
        this.responseQueue = responseQueue;
    }
}
