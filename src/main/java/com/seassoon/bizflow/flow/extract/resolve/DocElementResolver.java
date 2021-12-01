package com.seassoon.bizflow.flow.extract.resolve;

import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.ImgUtils;
import com.seassoon.bizflow.flow.extract.detect.Detector;
import com.seassoon.bizflow.flow.extract.detect.DocElementDetector;
import com.seassoon.bizflow.flow.extract.detect.HardWritingDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 文档元素提取
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocElementResolver extends AbstractResolver {

    private static final Logger logger = LoggerFactory.getLogger(DocElementDetector.class);

    private final Map<String, Detector> SEAL_ID_DETECTOR_MAP = new HashMap<>();
    private final static Integer expand = 30;

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private ApplicationContext appContext;

    @PostConstruct
    private void postConstruct() {
        // 初始化文档元素提取实例
        SEAL_ID_DETECTOR_MAP.put("1", appContext.getBean(HardWritingDetector.class));
    }

    @Override
    public Content resolve(Image image, Map<String, Object> params) {
        String formTypeId = (String) params.get("formTypeId");
        CheckpointConfig.ExtractPoint extractPoint = (CheckpointConfig.ExtractPoint) params.get("extractPoint");
        String strPath = image.getCorrected().getLocalPath();

        // 初始化返回值
        Content content = mapToContent(image.getImageId(), extractPoint);

        // 获取对应的文档元素提取器
        String signSealId = extractPoint.getSignSealId();
        Detector detector = SEAL_ID_DETECTOR_MAP.get(signSealId);
        if (detector == null) {
            logger.error("未找到对应的文档元素提取器，请检查checkpoint配置：formTypeId={}, field={}, signSealId={}",
                    formTypeId, extractPoint.getDocumentField(), signSealId);
            return content;
        }

        // 补充参数
        params.put("imageId", image.getImageId());
        params.put("threshold", properties.getAlgorithm().getElementMatchThreshold());
        // 计算检测位置的坐标
        List<List<Integer>> location = ImgUtils.calcLocation(strPath, extractPoint.getInitPosition());
        List<List<Integer>> detectArea = Arrays.asList(
                Arrays.asList(location.get(0).get(1), location.get(0).get(0)),
                Arrays.asList(location.get(1).get(1), location.get(1).get(0)));
        params.put("detectArea", detectArea);

        // 提取文档元素
        Field field = detector.detect(params);

        // 更新返回值（修正location）
        if (field != null) {
            ImgUtils.Shape shape = ImgUtils.getShape(strPath);
            List<List<Integer>> fieldLocation = field.getFieldLocation();
            int xMin = Math.max(0, fieldLocation.get(0).get(0) - expand),
                yMin = Math.max(0, fieldLocation.get(0).get(1) - expand),
                xMax = Math.min(fieldLocation.get(1).get(0) + expand, shape.getHeight()),
                yMax = Math.min(fieldLocation.get(1).get(1) + expand, shape.getWidth());
            field.setFieldLocation(Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax)));

            // 将field填入content
            content.setValueInfo(Collections.singletonList(field));
        }
        return content;
    }

    @Override
    public boolean support(CheckpointConfig.ExtractPoint extractPoint) {
        String signSealId = extractPoint.getSignSealId();
        if (StrUtil.isBlank(signSealId) || !extractPoint.getValueType().equals("img")) {
            return false;
        }
        return SEAL_ID_DETECTOR_MAP.containsKey(signSealId);
    }
}
