package com.seassoon.bizflow.flow.extract.strategy;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractStrategy implements UnifiedStrategy {

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {



        return null;
    }

    protected abstract Content parseContent(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig.ExtractPoint extractPoint);

    /**
     * 将一个{@link CheckpointConfig.ExtractPoint}对象转化为{@link Content}对象
     *
     * @param images       当前分类的图片列表
     * @param extractPoint {@link CheckpointConfig.ExtractPoint}提取点信息
     * @return {@link Content}，value_info是null，需要提取计算后填入
     */
    protected Content convert(List<Image> images, CheckpointConfig.ExtractPoint extractPoint) {
        Content content = new Content();

        // 根据页码匹配正确的image_id
        images.stream().filter(image -> image.getDocumentPage().equals(extractPoint.getPage())).findAny()
                .ifPresent(image -> content.setImageId(image.getImageId()));

        content.setDocumentField(extractPoint.getDocumentField());
        content.setPage(extractPoint.getPage());
        content.setImageOrString(Content.VALUE_TYPES.get(extractPoint.getValueType()));
        content.setSource("smj"); // 默认为扫描件
        content.setSortProperty(extractPoint.getSortProperty());
        content.setDisplayProperty(extractPoint.getDisplayProperty());
        return content;
    }
}
