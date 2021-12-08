package com.seassoon.bizflow.flow.extract.strategy;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.util.Collections3;
import com.seassoon.bizflow.flow.extract.resolve.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 多页提取策略
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class MultiPageStrategy extends AbstractStrategy {

    private static final Logger logger = LoggerFactory.getLogger(MultiPageStrategy.class);

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {
        String formTypeId = checkpoint.getFormTypeId();

        // 初始化参数列表
        Map<String, Object> params = new HashMap<>();
        params.put("images", images);
        params.put("formTypeId", checkpoint.getFormTypeId());

        // 对每个提取点分别提取
        return checkpoint.getExtractPoint().stream()
                .map(extractPoint -> mapToContent(params, extractPoint, images, ocrOutputs))
                .collect(Collectors.toList());
    }

    private Content mapToContent(Map<String, Object> params, CheckpointConfig.ExtractPoint extractPoint, List<Image> images,
                                 List<OcrOutput> ocrOutputs) {
        params.put("extractPoint", extractPoint);

        // 匹配支持的提取器
        Resolver resolver = getResolver(extractPoint);

        if (CollectionUtil.isEmpty(images) || resolver == null) {
            return Content.of(StrUtil.EMPTY, extractPoint);
        }

        // 匹配页码
        images = images.stream().filter(image -> image.getDocumentPage().equals(extractPoint.getPage())).collect(Collectors.toList());

        List<Content> contents = new ArrayList<>();
        // 多张材料循环提取
        for (Image image : images) {
            // 获取图片OCR结果
            OcrOutput ocr = ocrOutputs.stream()
                    .filter(ocrOutput -> ocrOutput.getImageName().equals(image.getImageId()))
                    .findAny().orElseThrow(() -> new NullPointerException("OCR结果丢失"));

            // 放入参数表
            params.put("image", image);
            params.put("ocr", ocr);

            // 提取内容
            Content content = resolver.resolve(params);
            if (content != null) {
                contents.add(content);
            }
        }

        // 合并多个结果
        if (CollectionUtil.isEmpty(contents)) {
            logger.error("材料{}字段{}信息提取失败", params.get("formTypeId"), extractPoint.getDocumentField());
            contents.add(Content.of(StrUtil.EMPTY, extractPoint));
        }

        Content content = contents.get(0);
        for (Content c : contents) {
            content.setValueInfo(Collections3.merge(content.getValueInfo(), c.getValueInfo()));
        }
        return content;
    }
}
