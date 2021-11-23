package com.seassoon.bizflow.core.component;

import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 一个简单的文件下载器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class FileDownloader {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloader.class);

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 下载url中的文件到指定目录
     *
     * @param urls 下载列表，{@link Pair}元素的集合，左边为文件名，右边为url
     * @param target 指定下载的目录
     * @return 下载后的本地文件列表
     */
    public List<String> download(List<Pair<String, String>> urls, String target) {
        logger.info("正在下载文件，共{}个文件", urls.size());
        AtomicInteger indexAI = new AtomicInteger(0);

        String recordId = BizFlowContextHolder.getInput().getRecordId();
        return urls.stream().parallel().map(pair -> {
            // 初始化线程上下文的recordID
            BizFlowContextHolder.putMDC(recordId);

            // 文件名和url
            String strFilename = pair.getLeft();
            String strUrl = pair.getRight();
            String strPath = null;

            ResponseEntity<byte[]> responseEntity = restTemplate.exchange(strUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), byte[].class);
            if (responseEntity.getStatusCode() != HttpStatus.OK && ArrayUtils.isEmpty(responseEntity.getBody())) {
                logger.error("文件{}下载失败：{}", strFilename, responseEntity);
            } else {
                try {
                    // 将文件保存到本地
                    Path path = Paths.get(target, strFilename);
                    if (Files.notExists(path.getParent())) {
                        Files.createDirectories(path.getParent());
                    }
                    Files.write(path, Objects.requireNonNull(responseEntity.getBody()));
                    logger.info("[{}/{}]个文件下载成功：{}", indexAI.incrementAndGet(), urls.size(), path);
                    strPath = path.toString();
                } catch (IOException e) {
                    logger.error("文件写入到本地失败 - " + e.getMessage(), e);
                }
            }
            return strPath;
        }).collect(Collectors.toList());
    }
}
