package com.seassoon.bizflow.flow.classify.special;

import com.google.common.collect.ImmutableMap;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.idcard.IdCardResponse;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Map;

/**
 * 描述:
 * 身份证检测
 *
 * @author chenningshi
 * @create 2021-12-02 14:09
 */
@Component
public class IdCardDetect extends SpecialDetect {


    @Override
    protected String specialClassif(BizFlowProperties properties) {
        return properties.getIntegration().get(BizFlowProperties.Service.ID_CARD);
    }
}
