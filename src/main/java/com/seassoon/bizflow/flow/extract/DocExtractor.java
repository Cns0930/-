package com.seassoon.bizflow.flow.extract;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.flow.extract.strategy.UnifiedStrategy;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Extractor}接口默认实现类，基于文档OCR结果和提取点提取结构化数据
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocExtractor implements Extractor {

    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(DocExtractor.class);

    /** 提取策略 */
    private final Map<String, UnifiedStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    private void postConstruct() {
//        strategyMap.put("", "");
    }

    @Override
    public List<DocumentKV> extract(List<Image> sortedImages, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints, Map<String, String> mapping) {
        // 从上下文获取RecordID
        String recordId = BizFlowContextHolder.getInput().getRecordId();

        // 按checkpoint提取，每个类别对应的图片可能有多张（多页）
        return checkpoints.stream().parallel().map(checkpoint -> {
            // 初始化线程上下文的recordID
            BizFlowContextHolder.putMDC(recordId);

            String formTypeId = checkpoint.getFormTypeId();

            // 获取对应分类下的图片
            List<Image> images = sortedImages.stream()
                    .filter(image -> image.getDocumentLabel().equals(formTypeId))
                    .collect(Collectors.toList());

            // 对应图片的OCR结果
            List<OcrOutput> imageOCRs = images.stream()
                    .map(image -> ocrOutputs.stream()
                            .filter(ocrOutput -> ocrOutput.getImageName().equals(image.getImageId()))
                            .findAny().orElse(null))
                    .collect(Collectors.toList());

            // 提取策略
            UnifiedStrategy strategy = strategyMap.get(mapping.get(formTypeId));

            return extractKV(images, imageOCRs, checkpoint, strategy);
        }).collect(Collectors.toList());
    }

    /**
     * 单个分类提取
     * @param images 对应分类的图片
     * @param ocrOutputs 图片OCR
     * @param checkpoint 提取点配置
     * @param strategy 提取策略
     * @return {@link DocumentKV}
     */
    private DocumentKV extractKV(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint, UnifiedStrategy strategy) {
        String formTypeId = checkpoint.getFormTypeId();
        logger.info("开始提取{}分类数据", formTypeId);

        // 计时器
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch(formTypeId);
        stopWatch.start();

        // TODO 提取的逻辑...

        stopWatch.stop();
        logger.info("材料{}提取完成，共耗时{}秒", formTypeId, stopWatch.getTotalTimeSeconds());
        return null;
    }
}
