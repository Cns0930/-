package com.seassoon.bizflow.flow.extract.resolve;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;

import java.util.Map;

/**
 * 提取文档信息具体内容的接口抽象
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Resolver {

    /**
     * 提取图片中的信息
     *
     * @param image  {@link Image}
     * @param params 参数
     * @return {@link Content}
     */
    Content resolve(Image image, Map<String, Object> params);

    /**
     * 根据{@link CheckpointConfig.ExtractPoint}判断是否支持这个提取器
     *
     * @param extractPoint {@link CheckpointConfig.ExtractPoint}
     * @return boolean value
     */
    boolean support(CheckpointConfig.ExtractPoint extractPoint);
}
