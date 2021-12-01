package com.seassoon.bizflow.flow.extract.strategy;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;

import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractStrategy implements UnifiedStrategy {

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {



        return null;
    }

    protected abstract Content parseContent(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig.ExtractPoint extractPoint);
}
