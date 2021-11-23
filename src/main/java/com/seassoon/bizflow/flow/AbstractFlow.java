package com.seassoon.bizflow.flow;

import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.Output;

import java.util.List;
import java.util.Map;

/**
 * 对流程处理器{@link Flow}进一步抽象，提供默认处理方式，包括：
 * 1.OCR识别
 * 2.文档分类
 * 3.结构化数据提取
 * 4.规则处理
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractFlow implements Flow {

    @Override
    public Output process(Input input) {

        // STEP-01.OCR结果识别
        List<OcrOutput> ocrOutputs = ocr(input.getImageList());

        // STEP-02.文档分类
        Map<String, List<Image>> sorted = sort(ocrOutputs, input);

        // STEP-03.结构化数据提取

        // STEP-04.规则解析

        return null;
    }

    /**
     * OCR识别
     *
     * @param images 图片列表
     * @return {@link OcrOutput}
     */
    protected abstract List<OcrOutput> ocr(List<Image> images);

    /**
     * 文档分类
     *
     * @param ocrOutputs OCR识别结果
     * @param input      input.json
     * @return 已分类的文档列表
     */
    protected abstract Map<String, List<Image>> sort(List<OcrOutput> ocrOutputs, Input input);
}
