package com.seassoon.bizflow.flow.extract.resolve;

import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文档元素提取
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocElementResolver implements Resolver {

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private HTTPCaller httpCaller;

    @Override
    public Content resolve(Image image, Map<String, Object> params) {



        return null;
    }

    @Override
    public boolean support(CheckpointConfig.ExtractPoint extractPoint) {
        return false;
    }
}
