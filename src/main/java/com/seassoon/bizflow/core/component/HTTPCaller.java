package com.seassoon.bizflow.core.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 提供HTTP相关调用工具方法
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class HTTPCaller {

    private static final Logger logger = LoggerFactory.getLogger(HTTPCaller.class);

    @Autowired
    private RestTemplate restTemplate;

    public <T> T post(String url, Map<String, Object> params, Class<T> clazz) {
        return post(url, params, MediaType.APPLICATION_JSON, clazz);
    }

    public <T> T post(String url, Map<String, Object> params, MediaType contentType, Class<T> clazz) {
        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        params.forEach(formData::add);

        ResponseEntity<T> responseEntity = restTemplate.postForEntity(url, new HttpEntity<>(formData, headers), clazz);
        return renderResponseEntity(responseEntity);
    }

    private <T> T renderResponseEntity(ResponseEntity<T> responseEntity) {
        // 非2xx开头的状态码都认为是失败
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            logger.error("调用HTTP失败：{}", responseEntity);
            return null;
        }
        return responseEntity.getBody();
    }
}
