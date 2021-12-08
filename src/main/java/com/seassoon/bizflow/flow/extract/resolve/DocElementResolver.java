package com.seassoon.bizflow.flow.extract.resolve;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.ImgUtils;
import com.seassoon.bizflow.flow.extract.detect.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

/**
 * 文档元素提取
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocElementResolver extends AbstractResolver implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DocElementDetector.class);

    private final Map<String, Detector> SEAL_ID_DETECTOR_MAP = new HashMap<>();
    private final static Integer expand = 30;

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private ApplicationContext appContext;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化文档元素提取实例
        SEAL_ID_DETECTOR_MAP.put("1", appContext.getBean(HandwritingDetector.class));
        SEAL_ID_DETECTOR_MAP.put("3", appContext.getBean(StampDetector.class));
        SEAL_ID_DETECTOR_MAP.put("4", appContext.getBean(AttachDetector.class));
        SEAL_ID_DETECTOR_MAP.put("7", appContext.getBean(HandwritingDetector.class));
        SEAL_ID_DETECTOR_MAP.put("13", appContext.getBean(CheckboxDetector.class));
        SEAL_ID_DETECTOR_MAP.put("14", appContext.getBean(StampDetector.class));
        SEAL_ID_DETECTOR_MAP.put("15_right", appContext.getBean(CheckboxDetector.class));
        SEAL_ID_DETECTOR_MAP.put("15_left", appContext.getBean(CheckboxDetector.class));
        SEAL_ID_DETECTOR_MAP.put("16", appContext.getBean(AttachDetector.class));
        SEAL_ID_DETECTOR_MAP.put("17", appContext.getBean(StampDetector.class));
    }

    @Override
    public Content resolve(Map<String, Object> params) {
        Image image = (Image) params.get("image");
        String formTypeId = (String) params.get("formTypeId");
        CheckpointConfig.ExtractPoint extractPoint = (CheckpointConfig.ExtractPoint) params.get("extractPoint");
        String strPath = image.getClassifiedPath();

        // 初始化返回值
        Content content = Content.of(image.getImageId(), extractPoint);

        // 获取对应的文档元素提取器
        String signSealId = extractPoint.getSignSealId();
        Detector detector = SEAL_ID_DETECTOR_MAP.get(signSealId);

        // 补充参数
        params.put("signSealId", signSealId);
        params.put("imageId", image.getImageId());
        params.put("threshold", properties.getAlgorithm().getElementMatchThreshold());
        // 计算检测位置的坐标，并切图
        ImgUtils.Shape shape = ImgUtils.getShape(strPath);
        List<List<Integer>> location = Arrays.asList(Arrays.asList(1, 1), Arrays.asList(shape.getHeight() - 1, shape.getWidth() - 1));
        if (CollectionUtil.isNotEmpty(extractPoint.getInitPosition())) {
            location = ImgUtils.calcLocation(shape, extractPoint.getInitPosition());
        }
        Path snapshot = snapshot(image, location);
        params.put("path", snapshot.toString());
        // TODO 应该缺一个已经提取到的 location

        // 检测区域
        List<List<Integer>> detectArea = Arrays.asList(
                Arrays.asList(location.get(0).get(1), location.get(0).get(0)),
                Arrays.asList(location.get(1).get(1), location.get(1).get(0)));
        params.put("detectArea", detectArea);

        // 提取文档元素
        Field field = detector.detect(params);

        // 更新返回值（修正location）
        if (field != null) {
            if (CollectionUtil.isNotEmpty(field.getFieldLocation())) {
                location = field.getFieldLocation();
                int xMin = Math.max(0, location.get(0).get(0) - expand),
                    yMin = Math.max(0, location.get(0).get(1) - expand),
                    xMax = Math.min(location.get(1).get(0) + expand, shape.getHeight()),
                    yMax = Math.min(location.get(1).get(1) + expand, shape.getWidth());
                location = Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax));
            }
            field.setFieldLocation(location);
        }
        // 将field填入content
        content.setValueInfo(Collections.singletonList(field));
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
