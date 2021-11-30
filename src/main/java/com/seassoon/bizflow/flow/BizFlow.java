package com.seassoon.bizflow.flow;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.LocalStorage;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.*;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.Input;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collector;
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
                    Files.copy(srcFile, path);

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
        logger.info("文档分类完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
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
        logger.info("文档数据提取完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return null;
    }

    /**
     * 合并结构化数据
     *
     * @param docKVs 提取结果
     * @param extraKVs 结构化数据
     * @param images 图片信息
     * @param checkpoints 提取配置
     */
    private void mergeExtraKV(List<DocumentKV> docKVs, List<ExtraKVInfo> extraKVs, List<Image> images, List<CheckpointConfig> checkpoints) {
        // 没有结构化数据  没有提取信息  跳过
        if(CollectionUtils.isEmpty(extraKVs) || CollectionUtils.isEmpty(docKVs)){
            return;
        }
        // 只取第一个（这是什么操作?）
        ExtraKVInfo extraKVInfo = extraKVs.get(0);
        // source不为超级帮办跳过  没有内容跳过
        if(Objects.isNull(extraKVInfo) || !"cjbb".equals(extraKVInfo.getSource()) ||
                CollectionUtils.isEmpty(extraKVInfo.getDocumentList())){
            return;
        }

        // 后面需要多次查找的数据先处理成map
        // documentLabel -> image
        Map<String, List<Image>> imageMap = images.stream().collect(Collectors.groupingBy(Image::getDocumentLabel));

        // documentLabel -> documentField -> fieldKV
        // 冲突时取前面的 防止运行不下去
        Map<String, Map<String, FieldKV>> kvMap = extraKVInfo.getDocumentList().stream()
                .collect(Collectors.toMap(Document::getDocumentLabel, document ->
                        document.getFieldVal().stream().collect(Collectors.toMap(FieldKV::getKey, fieldKV -> fieldKV, (f1, f2) -> f1)), (d1, d2) -> d1));

        // documentLabel -> checkpoint
        // 冲突时取前面的 防止运行不下去
        Map<String, CheckpointConfig> checkMap = checkpoints.stream().collect(Collectors.toMap(CheckpointConfig::getFormTypeId, checkpointConfig -> checkpointConfig, (c1, c2) -> c1));

        // 对提取结果遍历进行替换操作
        docKVs.forEach( documentKV -> {
            String documentLabel = documentKV.getDocumentLabel();
            List<Image> imageList = imageMap.getOrDefault(documentLabel, new ArrayList<>());
            Map<String, FieldKV> kv = kvMap.get(documentLabel);
            CheckpointConfig checkpointConfig = checkMap.get(documentLabel);
            List<Content> contentList = documentKV.getDocumentList();
            contentList.forEach( content -> {
                String documentField = content.getDocumentField();
                FieldKV fieldKV = kv.get(documentField);

                // 填入值
                if(fieldKV != null && !CollectionUtils.isEmpty(fieldKV.getVal())) {
                    // 如果匹配到
                    content.setSource(extraKVInfo.getSource());
                    // 如果有提取结果  替换为一个  取提取的第一个的坐标 和结构化的值
                    if(CollectionUtils.isEmpty(content.getValueInfo())){
                        content.setValueInfo(Arrays.asList(Field.of(fieldKV.getVal().get(0), calLocation(content.getValueInfo()), 1.0)));
                    }else{
                        // 如果没有提取结果  新增一条
                        content.setValueInfo(Arrays.asList(Field.of(fieldKV.getVal().get(0), null,1.0)));
                    }
                }

                // 填入image_id 和 document_page
                if(!StringUtils.hasLength(content.getImageId()) && !CollectionUtils.isEmpty(imageList)) {
                    imageList.stream().filter(image -> image.getDocumentSource() != null && image.getDocumentSource().equals(1)).findAny().ifPresent(image -> {
                        // 默认情况填充
                        content.setImageId(image.getImageId());
                        content.setPage(image.getDocumentPage());
                        // 多页情况 找到提取点 得到页码
                        // 用findFirst和算法原先逻辑一致 防止奇怪情况?
                        if (checkpointConfig != null && checkpointConfig.getMultiPage()) {
                            checkpointConfig.getExtractPoint().stream().filter(extractPoint ->
                                    documentField.equals(extractPoint.getDocumentField())).findFirst().flatMap(extractPoint ->
                                    imageList.stream().filter(img -> img.getDocumentSource() != null && image.getDocumentSource().equals(1) && img.getDocumentPage().equals(extractPoint.getPage())).findFirst()).ifPresent(img -> {
                                // 根据页码 找到正确的image
                                content.setImageId(img.getImageId());
                                content.setPage(img.getDocumentPage());
                            });
                        }

                    });
                }

                // 其他字段填充
                if(!StringUtils.hasLength(content.getIsTrue())){
                    // 字符串 true 是个什么操作
                    content.setIsTrue("true");
                }
                if(content.getPage() == null){
                    content.setPage(-1);
                }

            });
        });

    }

    /**
     * 合并结构化数据 - 合并坐标计算
     * @param valueInfo
     * @return
     */
    private List<List<Integer>> calLocation(List<Field> valueInfo){
        if(valueInfo.size() == 1){
            return valueInfo.get(0).getFieldLocation();
        }
        List<List<Integer>> location = new ArrayList<>();
        for(Field field : valueInfo){
            if(field.getFieldLocation() != null && field.getFieldLocation().size() == 2){
                if(location.isEmpty()){
                    location.add(field.getFieldLocation().get(0));
                }else if(location.size() == 1){
                    location.add(field.getFieldLocation().get(1));
                }else {
                    // 替换第二个元素
                    location.remove(1);
                    location.add(field.getFieldLocation().get(1));
                }
            }
        }
        return location;
    }

    @Override
    protected List<Approval> rule(List<DocumentKV> docKVs, List<RuleConfig> rules) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        List<Approval> approvals = ruleEngine.process(docKVs, rules);

        localStorage.save(docKVs, "rule_approvals.json");

        stopWatch.stop();
        logger.info("规则引擎执行完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return approvals;
    }
}
