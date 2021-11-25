package com.seassoon.bizflow.core.component.ocr;

import com.seassoon.bizflow.config.BizFlowProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;

/**
 * OCR识别的paddle实现，通过http方式调用
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class PaddleOcrProcessor implements OcrProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PaddleOcrProcessor.class);

    private final RestTemplate restTemplate;
    private final String url;

    public PaddleOcrProcessor(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    @Override
    public String process(Path file) {

        // 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 将图片绑定到form-data
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("file", new FileSystemResource(file));

        // 构建请求实例，发送，并获取结果
        HttpEntity<MultiValueMap<String, Object>> body = new HttpEntity<>(formData, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, body, String.class);
        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            logger.error("图片{}OCR识别失败：{}", file.getFileName(), responseEntity);
            return null;
        }

        return responseEntity.getBody();
    }
}
