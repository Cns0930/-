package com.seassoon.bizflow.flow.classify.special;

import com.seassoon.bizflow.core.model.ocr.Image;

public interface SpecialServer {
    /**
     * 特殊证件分类
     *
     * @param image 图片
     */
    boolean preProcessing(Image image);
}
