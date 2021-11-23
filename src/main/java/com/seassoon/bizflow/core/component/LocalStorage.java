package com.seassoon.bizflow.core.component;

import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 提供本地缓存功能
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class LocalStorage {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorage.class);

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private ApplicationContext appContext;

    /**
     * 上传minio执行的线程池：
     * 1.核心线程数为1，最大线程数为2
     * 2.等待30秒，若30秒没有处理完，让出当前线程
     * 3.队列最大数为5，根据需要自行调整
     */
    private final ExecutorService executor = new ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5));

    /**
     * 将对象转成JSON并保存到本地临时目录
     *
     * @param object 转JSON的对象
     * @param filename 文件名
     */
    public void save(Object object, String filename) {
        // 对象转成JSON字符串，pretty_print格式化输出
        String jsonStr = JSONUtils.writeValueAsString(object, true);
        if (jsonStr == null) {
            return;
        }

        // 从BizFlow上下文获取RecordID
        String recordId = BizFlowContextHolder.getInput().getRecordId();
        LocalDateTime timestamp = BizFlowContextHolder.getTimestamp();
        Path path = Paths.get(properties.getLocalStorage(), recordId, "json", filename);

        try {
            // 创建目录
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }

            // 写入文件内容
            Files.write(path, jsonStr.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("JSON文件保存失败 - " + e.getMessage(), e);
        }

        // 上传到minio
        if (properties.getMinio().getEnabled()) {
            String bucket = properties.getMinio().getBucket().get(BizFlowProperties.Minio.Bucket.DEFAULT);
            MinioClient minioClient = appContext.getBean(MinioClient.class);

            // 格式化timestamp
            String strDate = DateTimeFormatter.ofPattern("yyyyMMdd").withLocale(Locale.CHINESE).withZone(ZoneId.systemDefault()).format(timestamp);
            String strTime = DateTimeFormatter.ofPattern("HHmmss").withLocale(Locale.CHINESE).withZone(ZoneId.systemDefault()).format(timestamp);
            String strObjectName = String.format("job/ai/%s/%s/%s/%s", strDate, recordId, strTime, filename);

            // 上传minio这个操作比较耗时，所以用异步方式上传
            executor.submit(() -> {
                BizFlowContextHolder.putMDC(recordId);
                try (InputStream inputStream = Files.newInputStream(path)) {
                    PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                            .bucket(bucket)
                            .contentType("application/octet-stream")
                            .object(strObjectName)
                            .stream(inputStream, Files.size(path), -1)
                            .build();
                    ObjectWriteResponse response = minioClient.putObject(putObjectArgs);
                    if (StringUtils.isNotBlank(response.region())) {
                        logger.info("文件{}已成功上传到{}", filename, response.region());
                    }
                } catch (Exception e) {
                    logger.error("JSON文件上传到minio出错 - " + e.getMessage(), e);
                }
            });
        }
    }
}
