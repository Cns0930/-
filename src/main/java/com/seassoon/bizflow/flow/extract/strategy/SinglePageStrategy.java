package com.seassoon.bizflow.flow.extract.strategy;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 单页提取策略
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class SinglePageStrategy implements UnifiedStrategy {

    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(SinglePageStrategy.class);

    @Override
    public List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint) {
        String formTypeId = checkpoint.getFormTypeId();

        // 检查是否为多页
        if (checkpoint.getMultiPage()) {
            logger.error("材料{}仅支持单页提取，请检查事项配置", formTypeId);
            return new ArrayList<>();
        }

        // 单页多分材料



        return null;
    }
}
