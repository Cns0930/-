package com.seassoon.bizflow.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSONUtils
 * @author lw900925 (liuwei@seassoon.com)
 */
public class JSONUtils {

    private static final Logger logger = LoggerFactory.getLogger(JSONUtils.class);
    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .build();

    private JSONUtils() {
        // do not initialize
    }

    public static <T> T readValue(String jsonStr, Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonStr, valueType);
        } catch (JsonProcessingException e) {
            logger.error("JSON读取失败 - " + e.getMessage(), e);
            return null;
        }
    }

    public static <T> T readValue(String jsonStr, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(jsonStr, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("JSON读取失败 - " + e.getMessage(), e);
            return null;
        }
    }

    public static JsonNode readTree(String jsonStr) {
        try {
            return objectMapper.readTree(jsonStr);
        } catch (JsonProcessingException e) {
            logger.error("JSON读取失败 - " + e.getMessage(), e);
            return null;
        }
    }

    public static String writeValueAsString(Object obj) {
        return writeValueAsString(obj, false);
    }

    public static String writeValueAsString(Object obj, boolean prettyPrint) {
        try {
            ObjectWriter objectWriter = objectMapper.writer();
            if (prettyPrint) {
                objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
            }
            return objectWriter.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败 - " + e.getMessage(), e);
            return null;
        }
    }
}
