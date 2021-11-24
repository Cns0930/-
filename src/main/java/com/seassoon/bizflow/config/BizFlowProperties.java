package com.seassoon.bizflow.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
@ConfigurationProperties(prefix = "biz-flow")
@Data
@EqualsAndHashCode
public class BizFlowProperties {

    private String localStorage;
    private Map<Service, String> integration = new HashMap<>();
    private OcrType ocrType;
    private Redis redis = new Redis();
    private Minio minio = new Minio();
    private Schedule schedule = new Schedule();
    private Algorithm algorithm = new Algorithm();

    public enum Service {
        /** ocr识别 */
        OCR,

        /** 手写字检测（或者签字检测） */
        HAND_WRITING_TEXT,

        /** 日期检测服务（打印日期或者手写日期） */
        DATE_TEXT,

        /** 检测盖章(基于FCN) */
        SEAL,

        /** 检测盖章(基于SSD) */
        STAMP,

        /** 打勾检测 */
        CHECK_BOX,

        /** 是否粘贴身份证 */
        ID_CARD,

        /** 身份证信息提取 */
        ID_CARD_EXTRACT,

        /** 旋转图像校正 */
        ORIENTATION,

        /** 是否粘贴驾驶证 */
        DRIVER_LICENSE,

        /** ner信息提取 */
        NER,

        /** 检测综合文档元素（是否有签字、盖章、日期、身份证） */
        DOC_ELEMENT,
    }

    public enum OcrType {
        PADDLE,
        REDIS,
        ;
    }

    /**
     * Redis配置
     */
    @Data
    @EqualsAndHashCode
    public static class Redis {
        private String host;
        private Integer port;
        private String password;
        private List<Integer> databases = new ArrayList<>();
        private Duration timeout;
        private Map<Queue, String> queue = new HashMap<>();

        /**
         * Redis队列枚举
         */
        public enum Queue {
            /** 待处理事项 */
            TODO,

            /** 处理完成的事项 */
            FLASH,

            /** ocr request */
            OCR_REQUEST,

            /** ocr response */
            OCR_RESPONSE,

            ;
        }
    }

    /**
     * Minio配置
     */
    @Data
    @EqualsAndHashCode
    public static class Minio {
        private Boolean enabled = true;
        private String endpoint;
        private Integer port;
        private String accessKey;
        private String secretKey;
        private String url;
        private Map<Bucket, String> bucket = new HashMap<>();

        /**
         * Minio Bucket枚举
         */
        public enum Bucket {
            /** 默认 */
            DEFAULT,

            ;
        }
    }

    /**
     * 定时任务调度配置
     */
    @Data
    @EqualsAndHashCode
    public static class Schedule {
        private Integer poolSize;
        private String threadNamePrefix = "biz-flow-scheduling-";
        private Boolean waitForTaskCompletedOnShutdown = true;
        private Long awaitTerminationSeconds = 30L;
    }

    /**
     * 算法配置
     */
    @Data
    @EqualsAndHashCode
    public static class Algorithm {
        private Integer dotCoincide;
        private Double matchThreshold;
    }
}
