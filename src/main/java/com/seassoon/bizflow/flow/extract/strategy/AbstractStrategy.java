package com.seassoon.bizflow.flow.extract.strategy;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.util.Collections3;
import com.seassoon.bizflow.flow.extract.resolve.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractStrategy implements UnifiedStrategy, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SinglePageStrategy.class);

    private List<Resolver> resolvers = new ArrayList<>();

    @Autowired
    private ApplicationContext appContext;

    /**
     * 初始化所有提取工具{@link Resolver}
     *
     * @throws Exception 异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Resolver> namedResolvers = appContext.getBeansOfType(Resolver.class);
        if (MapUtil.isEmpty(namedResolvers)) {
            throw new NullPointerException("没有可用的Resolver");
        }
        resolvers = new ArrayList<>(namedResolvers.values());
    }

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {
        String formTypeId = checkpoint.getFormTypeId();

        // 初始化参数列表
        Map<String, Object> params = new HashMap<>();
        params.put("images", images);
        params.put("formTypeId", checkpoint.getFormTypeId());

        // 对每个提取点分别提取
        return checkpoint.getExtractPoint().stream()
                .map(extractPoint -> {
                    params.put("extractPoint", extractPoint);

                    // 检查image列表
                    if (CollectionUtil.isEmpty(images)) {
                        logger.error("未找到材料[{}]的分类图片", formTypeId);
                        return Content.of(StrUtil.EMPTY, extractPoint);
                    }

                    // 匹配正确的提取器
                    Resolver resolver = findResolver(extractPoint);
                    if (resolver == null) {
                        logger.error("材料[{}]字段[{}]未匹配到提取器，请检查checkpoint配置", params.get("formTypeId"), extractPoint.getDocumentField());
                        return Content.of(StrUtil.EMPTY, extractPoint);
                    }

                    // 匹配页码，如果又多张材料（如单页多份）遍历每张材料提取
                    List<Content> contents = images.stream().filter(image -> image.getDocumentPage().equals(extractPoint.getPage()))
                            .map(image -> {
                                // 获取图片OCR结果
                                OcrOutput ocr = ocrOutputs.stream()
                                        .filter(ocrOutput -> ocrOutput.getImageName().equals(image.getImageId()))
                                        .findAny().orElseThrow(() -> new NullPointerException("OCR结果丢失"));

                                // 放入参数表
                                params.put("image", image);
                                params.put("ocr", ocr);

                                // 调用提取工具
                                Content content = resolver.resolve(params);

                                // 处理结果
                                if (content == null) {
                                    logger.error("材料[{}]字段[{}]信息提取失败", formTypeId, extractPoint.getDocumentField());
                                    content = Content.of(image.getImageId(), extractPoint);
                                }

                                return content;
                            }).collect(Collectors.toList());


                    // 单页多份材料提取结果又多个，需要做合并（只合并value_info属性）
                    List<Field> fields = contents.stream().map(Content::getValueInfo).reduce(Collections3::merge).orElse(Lists.newArrayList());
                    Content content = contents.stream().findFirst().orElse(Content.of(StrUtil.EMPTY, extractPoint));
                    content.setValueInfo(fields);
                    return content;
                }).collect(Collectors.toList());
    }

    /**
     * 根据{@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}信息匹配到正确的{@link Resolver}
     *
     * @param extractPoint {@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}
     * @return {@link Resolver}集合
     */
    protected Resolver findResolver(CheckpointConfig.ExtractPoint extractPoint) {
        return resolvers.stream().filter(resolver -> resolver.support(extractPoint)).findAny().orElse(null);
    }
}
