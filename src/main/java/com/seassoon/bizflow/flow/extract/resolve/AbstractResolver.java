package com.seassoon.bizflow.flow.extract.resolve;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;

import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractResolver implements Resolver {

    @Override
    public Content resolve(Image image, Map<String, Object> params) {
        return null;
    }

    // 该方法由子类实现
    @Override
    public abstract boolean support(CheckpointConfig.ExtractPoint extractPoint);

    /**
     * 将一个{@link CheckpointConfig.ExtractPoint}对象转化为{@link Content}对象
     *
     * @param imageId      图片ID
     * @param extractPoint {@link CheckpointConfig.ExtractPoint}提取点信息
     * @return {@link Content}，value_info是null，需要提取计算后填入
     */
    protected Content mapToContent(String imageId, CheckpointConfig.ExtractPoint extractPoint) {
        Content content = new Content();
        content.setImageId(imageId);
        content.setDocumentField(extractPoint.getDocumentField());
        content.setPage(extractPoint.getPage());
        content.setImageOrString(Content.VALUE_TYPES.get(extractPoint.getValueType()));
        content.setSource("smj"); // 默认为扫描件
        content.setSortProperty(extractPoint.getSortProperty());
        content.setDisplayProperty(extractPoint.getDisplayProperty());
        return content;
    }
}
