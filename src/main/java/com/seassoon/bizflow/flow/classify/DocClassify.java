package com.seassoon.bizflow.flow.classify;

import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.extra.ExtraKVInfo;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;

/**
 * 文档分类器接口
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface DocClassify {

    /**
     * 根据OCR结果对文档分类
     *
     * @param ocrOutputs {@link List<OcrOutput>} OCR输出结果
     * @param images     待分类的图片列表
     * @param sortConfig 事项的分类配置
     * @param extraKVs   结构化数据
     * @return 分类结果（K:材料类别；V:分类材料）
     */
    List<Image> classify(List<OcrOutput> ocrOutputs, List<Image> images, SortConfig sortConfig, List<ExtraKVInfo> extraKVs);
}
