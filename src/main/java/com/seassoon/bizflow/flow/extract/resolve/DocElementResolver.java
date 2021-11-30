package com.seassoon.bizflow.flow.extract.resolve;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文档元素提取
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocElementResolver implements Resolver {

    private final List<String> supportedSealIds = Arrays.asList("1", "2", "3", "4", "5", "6", "8", "9", "10", "11", "12", "20");

    @Override
    public Content resolve(Image image, Map<String, Object> params) {



        return null;
    }

    @Override
    public boolean support(CheckpointConfig.ExtractPoint extractPoint) {
        String signSealId = extractPoint.getSignSealId();
        if (StrUtil.isBlank(signSealId)) {
            return false;
        }
        return supportedSealIds.contains(signSealId);
    }
}
