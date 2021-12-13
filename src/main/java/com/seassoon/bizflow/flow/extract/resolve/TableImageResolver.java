package com.seassoon.bizflow.flow.extract.resolve;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.Shape;
import com.seassoon.bizflow.core.util.ImgUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 材料为图片，且包含表格的提取器。<p>
 * 位于表格中的截图，key:value, value 位于key上下左右侧，多页
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class TableImageResolver extends AbstractResolver {

    private static final Logger logger = LoggerFactory.getLogger(TableImageResolver.class);


    @Override
    public Content resolve(Map<String, Object> params) {
        // 获取参数
        String formTypeId = (String) params.get("formTypeId");
        CheckpointConfig.ExtractPoint extractPoint = (CheckpointConfig.ExtractPoint) params.get("extractPoint");
        Image image = (Image) params.get("image");
        OcrOutput ocr = (OcrOutput) params.get("ocr");

        // 检查
        checkPage(extractPoint, image, formTypeId);
        checkAlias(extractPoint, formTypeId);

        // 图片分辨率&相对位置
        Shape shape = ImgUtils.getShape(image.getCorrected().getLocalPath());
        List<List<Integer>> location = ImgUtils.calcLocation(shape, extractPoint.getInitPosition());

        // 初始化返回值
        Content content = Content.of(image.getImageId(), extractPoint);


        return content;
    }

    @Override
    public boolean support(CheckpointConfig.ExtractPoint extractPoint) {
        return "table".equals(extractPoint.getValueEnvironment())
                && Arrays.asList("up", "down", "left", "right").contains(extractPoint.getKeyValueRelativePosition())
                && "img".equals(extractPoint.getValueType());
    }
}
