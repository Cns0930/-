package com.seassoon.bizflow.flow;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.LocalStorage;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.rule.Approval;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.flow.classify.DocClassify;
import com.seassoon.bizflow.flow.extract.Extractor;
import com.seassoon.bizflow.flow.ocr.OCR;
import com.seassoon.bizflow.flow.rule.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 默认预审流程处理器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class BizFlow extends AbstractFlow {

    private static final Logger logger = LoggerFactory.getLogger(BizFlow.class);

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private LocalStorage localStorage;
    @Autowired
    private OCR ocr;
    @Autowired
    private DocClassify docClassify;
    @Autowired
    private Extractor extractor;
    @Autowired
    private RuleEngine ruleEngine;

    @Override
    protected List<OcrOutput> ocr(List<Image> images) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 处理图片OCR
        List<OcrOutput> ocrOutputs = ocr.ocr(images);

        // 保存本地JSON
        localStorage.save(ocrOutputs, "ocr_output.json");

        stopWatch.stop();
        logger.info("图片OCR处理完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return ocrOutputs;
    }

    @Override
    protected List<Image> sort(List<OcrOutput> ocrOutputs, Input input) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 图片分类
        List<Image> images = docClassify.classify(ocrOutputs, input.getImageList(), input.getConfig().getSortConfig(), input.getExtraInfo());

        // 已下载的原始图片
        try {
            Path src = Paths.get(properties.getLocalStorage(), input.getRecordId(), "/files/src");
            List<Path> srcFiles = Files.list(src).collect(Collectors.toList());

            // 保存已分类的图片到本地
            for (Image image : images) {
                // 目标路径，如果不存在就新建
                Path target = Paths.get(properties.getLocalStorage(), input.getRecordId(), "/files/classified", image.getDocumentLabel());
                if (Files.notExists(target)) {
                    Files.createDirectories(target);
                }

                // 在src中找到对应的图片
                Path srcFile = srcFiles.stream()
                        .filter(file -> FileUtil.mainName(file.getFileName().toFile()).equals(image.getImageId()))
                        .findAny().orElse(null);

                if (srcFile != null) {
                    // 复制到target目录
                    Path path = Paths.get(target.toString(), srcFile.getFileName().toString());
                    Files.copy(srcFile, path);

                    // 更新image对象属性
                    image.setClassifiedPath(path.toString());
                }
            }
        } catch (IOException e) {
            logger.error("保存已分类图片出错 - " + e.getMessage(), e);
        }

        // 保存分类结果
        localStorage.save(images, "classified_images.json");

        stopWatch.stop();
        logger.info("文档分类完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return images;
    }

    @Override
    protected List<DocumentKV> extract(List<Image> sortedImages, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints, Map<String, String> mapping) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 提取结构化数据
        List<DocumentKV> docKVs = extractor.extract(sortedImages, ocrOutputs, checkpoints, mapping);

        // 保存提取结果
        localStorage.save(docKVs, "doc_kv.json");

        stopWatch.stop();
        logger.info("文档数据提取完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return null;
    }

    @Override
    protected List<Approval> rule(List<DocumentKV> docKVs, List<RuleConfig> ruleConfigs) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        List<Approval> approvals = ruleEngine.process(docKVs, ruleConfigs);

        localStorage.save(docKVs, "rule_approvals.json");

        stopWatch.stop();
        logger.info("规则引擎执行完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return approvals;
    }
}
