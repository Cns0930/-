package com.seassoon.bizflow.core.component.ocr;

import java.nio.file.Path;

/**
 * 图片OCR处理服务
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface OcrProcessor {

    /**
     * 处理图片OCR识别
     *
     * @param file 图片文件路径
     * @return OCR识别结果
     */
    String process(Path file);
}
