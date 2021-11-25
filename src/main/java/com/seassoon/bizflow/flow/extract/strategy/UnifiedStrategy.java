package com.seassoon.bizflow.flow.extract.strategy;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;

/**
 * 文档提取策略
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface UnifiedStrategy {
    /**
     * 解析并提取数据集
     *
     * @param images     图片列表
     * @param ocrOutputs 图片OCR结果
     * @param checkpoint 提取点配置
     * @return {@link DocumentKV}
     */
    List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint);
}
