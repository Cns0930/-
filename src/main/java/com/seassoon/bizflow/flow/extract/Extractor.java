package com.seassoon.bizflow.flow.extract;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;
import java.util.Map;

/**
 * 文档结构化数据提取器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Extractor {

    /**
     * 提取文档结构化数据
     *
     * @param sortedImages 已分类的图片
     * @param ocrOutputs   图片OCR结果
     * @param checkpoints  事项配置-提取点
     * @param mapping      事项配置-提取策略映射
     * @return 结构化数据列表
     */
    List<DocumentKV> extract(List<Image> sortedImages, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints, Map<String, String> mapping);
}
