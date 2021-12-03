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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * 单页提取策略
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class SinglePageStrategy extends AbstractStrategy {

    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(SinglePageStrategy.class);

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {
        String formTypeId = checkpoint.getFormTypeId();

        // 检查是否为多页
        if (checkpoint.getMultiPage()) {
            logger.error("材料{}仅支持单页提取，请检查事项配置", formTypeId);
            return new ArrayList<>();
        }

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
        List<Resolver> resolvers = forResolvers(extractPoint);

        return images.stream().map(image -> {
            // 获取图片OCR结果
            OcrOutput ocr = ocrOutputs.stream()
                    .filter(ocrOutput -> ocrOutput.getImageName().equals(image.getImageId()))
                    .findAny().orElseThrow(() -> new NullPointerException("OCR结果丢失"));

            // 放入参数表
            params.put("image", image);
            params.put("ocr", ocr);

            // 提取内容
            return doResolve(params, resolvers, image, ocr);
        }).findAny().orElseThrow(() -> new NullPointerException(String.format("材料%s字段%s信息提取失败", params.get("formTypeId"), extractPoint.getDocumentField())));
    }

    private Content doResolve(Map<String, Object> params, List<Resolver> resolvers, Image image, OcrOutput ocr) {
        // 调用Resolver对象提取字段内容
        return resolvers.stream()
                .map(resolver -> resolver.resolve(params))
                .filter(content -> CollectionUtil.isNotEmpty(content.getValueInfo()))
                .collect(Collectors.toMap(Content::getDocumentField, content -> content, (a, b) -> {
                    // 对于单页跨页的图片分别提取后，存在多个Content，需要做一次合并，合并的只是valueInfo属性，其他不变
                    a.setValueInfo(Collections3.merge(a.getValueInfo(), b.getValueInfo()));
                    return a;
                })).values().stream().findAny().orElseGet(() -> {
                    // 如果没有提取到返回一个valueInfo为空的Content
                    CheckpointConfig.ExtractPoint extractPoint = (CheckpointConfig.ExtractPoint) params.get("extractPoint");
                    return Content.of(image.getImageId(), extractPoint);
                });
    }
}
