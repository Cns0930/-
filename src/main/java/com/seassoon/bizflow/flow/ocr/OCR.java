package com.seassoon.bizflow.flow.ocr;

import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;

/**
 * 图片OCR识别处理器
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface OCR {

    /**
     * OCR识别
     *
     * @param images 图片列表
     * @return {@link OcrOutput}
     */
    List<OcrOutput> ocr(List<Image> images);
}
