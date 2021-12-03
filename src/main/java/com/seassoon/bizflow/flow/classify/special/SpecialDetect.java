package com.seassoon.bizflow.flow.classify.special;

import com.google.common.collect.ImmutableMap;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.component.ocr.OcrProcessor;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.Map;

/**
 * 处理特殊分类接口
 */
public abstract class SpecialDetect implements SpecialServer {

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private HTTPCaller httpCaller;

    @Override
    public boolean preProcessing(Image image) {
        // 从上下文获取RecordID
        String recordId = BizFlowContextHolder.getInput().getRecordId();
        // 1.获取图片保存路径
        String imagePath = Paths.get(properties.getLocalStorage(), recordId, "/files/src/" + image.getImageId() + ".jpg").toString();
        String url = specialClassif(properties);
        // 要上传的图片
        Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("file", new FileSystemResource(imagePath))
                .build();
        String response = httpCaller.post(url, params, MediaType.MULTIPART_FORM_DATA, String.class);
        return StringUtils.isNotEmpty(response) && !response.equals("[]\n") ? true : false;
    }

    /**
     * 匹配对应处理器
     *
     * @param properties 属性
     */
    protected abstract String specialClassif(BizFlowProperties properties);
}
