package com.seassoon.bizflow.flow.extract;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;
import java.util.Map;

/**
 * 临时方案：调用老版本Python提供的提取接口
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class DummyExtractor implements Extractor {

    @Override
    public List<DocumentKV> extract(List<Image> sortedImages, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints, Map<String, String> mapping) {

        return null;
    }
}
