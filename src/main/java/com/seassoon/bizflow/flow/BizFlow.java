package com.seassoon.bizflow.flow;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.LocalStorage;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.*;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.rule.Approval;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.flow.classify.DocClassify;
import com.seassoon.bizflow.flow.extract.Extractor;
import com.seassoon.bizflow.flow.ocr.OCR;
import com.seassoon.bizflow.flow.rule.RuleEngine;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        logger.info("#step1:图片OCR处理完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return ocrOutputs;
    }

    @Override
    protected List<Image> sort(List<Image> images, List<OcrOutput> ocrOutputs, SortConfig sortConfig, List<ExtraKVInfo> extraKVs) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 图片分类
        List<Image> typedImages = docClassify.classify(ocrOutputs, images, sortConfig, extraKVs);

        // 已下载的原始图片
        try {
            String recordId = BizFlowContextHolder.getInput().getRecordId();;
            Path src = Paths.get(properties.getLocalStorage(), recordId, "/files/src");
            List<Path> srcFiles = Files.list(src).collect(Collectors.toList());

            // 保存已分类的图片到本地
            for (Image image : typedImages) {
                // 目标路径，如果不存在就新建
                Path target = Paths.get(properties.getLocalStorage(), recordId, "/files/classified", image.getDocumentLabel());
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
                    if (Files.notExists(path)) {
                        Files.copy(srcFile, path);
                    }
                    // 更新image对象属性
                    image.setClassifiedPath(path.toString());
                }
            }
        } catch (IOException e) {
            logger.error("保存已分类图片出错 - " + e.getMessage(), e);
        }

        // 保存分类结果
        localStorage.save(typedImages, "classified_images.json");

        stopWatch.stop();
        logger.info("#step2:文档分类完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return typedImages;
    }

    @Override
    protected List<DocumentKV> extract(List<Image> images, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints,
                                       Map<String, String> mapping, List<ExtraKVInfo> extraKVs) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 提取结构化数据
        List<DocumentKV> docKVs = extractor.extract(images, ocrOutputs, checkpoints, mapping);

        // 如果帮办有结构化数据，合并到结果中
        mergeExtraKV(docKVs, extraKVs, images, checkpoints);

        // 保存提取结果
        localStorage.save(docKVs, "doc_kv.json");

        stopWatch.stop();
        logger.info("#step3:文档数据提取完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return null;
    }

    private void mergeExtraKV(List<DocumentKV> docKVs, List<ExtraKVInfo> extraKVs, List<Image> images, List<CheckpointConfig> checkpoints) {
        // 只合并source为超级帮办的
        if (CollectionUtil.isEmpty(extraKVs) || extraKVs.get(0) == null
                || !"cjbb".equals(extraKVs.get(0).getSource()) && extraKVs.get(0).getDocumentList() == null) {
            return;
        }

        // 结构化数据Key-Value映射，第一个Key为材料编号，第二个Key为字段名称
        Map<String, Map<String, String>> extraKVMap = extraKVs.get(0).getDocumentList().stream()
                .collect(Collectors.toMap(Document::getDocumentLabel,
                        document -> document.getFieldVal().stream().collect(Collectors.toMap(FieldKV::getKey, fieldKV -> fieldKV.getVal().get(0), (a, b) -> a)),
                        (a, b) -> a));

        // 分类图片映射，Key为材料编号，Value为图片列表
        Map<String, List<Image>> imageMap = images.stream().collect(Collectors.groupingBy(Image::getDocumentLabel));

        // 提取点页码映射，第一个Key为材料编号，第二个Key为字段名称，目的是为了得到指定材料编号下指定字段名称的页码
        Map<String, Map<String, Integer>> checkpointPageMap = checkpoints.stream()
                .collect(Collectors.toMap(CheckpointConfig::getFormTypeId,
                        checkpointConfig -> checkpointConfig.getExtractPoint().stream()
                                .collect(Collectors.toMap(CheckpointConfig.ExtractPoint::getDocumentField, CheckpointConfig.ExtractPoint::getPage, (a, b) -> a)),
                        (a, b) -> a));

        // 替换每条结构化数据
        docKVs.forEach(documentKV -> documentKV.getDocumentList().forEach(document -> {
            document.setSource(extraKVs.get(0).getSource());
            document.setPage(checkpointPageMap.getOrDefault(documentKV.getDocumentLabel(), Maps.newHashMap())
                    .getOrDefault(document.getDocumentField(), document.getPage()));

            // 根据页码匹配图片ID
            String imageId = imageMap.getOrDefault(documentKV.getDocumentLabel(), Lists.newArrayList()).stream()
                    .filter(image -> image.getDocumentSource() == 1 && image.getDocumentPage().equals(document.getPage()))
                    .map(Image::getImageId).findAny().orElse(document.getImageId());
            document.setImageId(imageId);

            // 将提取内容替换为超级帮办的值
            String defaultContent = document.getValueInfo().stream().findFirst().orElse(Field.of(null, null, 1.0)).getFieldContent();
            String fieldContent = extraKVMap.getOrDefault(documentKV.getDocumentLabel(), Maps.newHashMap())
                    .getOrDefault(document.getDocumentField(), defaultContent);
            Field field = Field.of(fieldContent, null, 1.0);
            if (CollectionUtil.isNotEmpty(document.getValueInfo())) {
                // 如果有多个坐标，合并成一个大坐标（第一个的起始位置和最后一个的结束位置）
                List<Integer> start = CollectionUtil.getFirst(document.getValueInfo()).getFieldLocation().get(0);
                List<Integer> end = CollectionUtil.getLast(document.getValueInfo()).getFieldLocation().get(1);
                field.setFieldLocation(Arrays.asList(start, end));
            }
            document.setValueInfo(Collections.singletonList(field));
        }));
    }

    @Override
    protected List<Approval> rule(List<DocumentKV> docKVs, List<RuleConfig> rules) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        List<Approval> approvals = ruleEngine.process(docKVs, rules);

        localStorage.save(docKVs, "rule_approvals.json");

        stopWatch.stop();
        logger.info("#step4:规则引擎执行完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return approvals;
    }
}
