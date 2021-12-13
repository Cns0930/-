package com.seassoon.bizflow.flow.extract.resolve;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.ImgUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;

import javax.xml.bind.ValidationException;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractResolver implements Resolver {

    private static final Logger logger = LoggerFactory.getLogger(AbstractResolver.class);

    @Autowired
    private BizFlowProperties properties;

    @Override
    public abstract Content resolve(Map<String, Object> params);

    // 该方法由子类实现
    @Override
    public abstract boolean support(CheckpointConfig.ExtractPoint extractPoint);

    /**
     * 对图片指定位置截图
     *
     * @param image    图片{@link Image}对象
     * @param location 截图位置
     * @return 截图后保存路径
     */
    protected Path snapshot(Image image, List<List<Integer>> location) {
        String recordId = BizFlowContextHolder.getInput().getRecordId();

        // 截图坐标（x和y分别未起始位置的坐标，width和height分别为要截图的宽和高）
        int x = location.get(0).get(1),
                y = location.get(0).get(0),
                width = location.get(1).get(1) - x,
                height = location.get(1).get(0) - y;
        Rectangle rectangle = new Rectangle(x, y, width, height);

        // 截图保存位置（文件命名规则：起始高-起始宽_结束高-结束宽）
        String strFilename = y + "-" + x + "_" + location.get(1).get(0) + "-" + location.get(1).get(1) + ".jpg";
        Path snapshot = Paths.get(properties.getLocalStorage(), recordId, "files/snapshot", image.getDocumentLabel(), image.getImageId(), strFilename);
        ImgUtils.cut(Paths.get(image.getClassifiedPath()).toFile(), snapshot.toFile(), rectangle);
        return snapshot;
    }

    /**
     * 检查extractPoint页码和image页码是否一致
     *
     * @param extractPoint {@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}
     * @param formTypeId   材料分类
     */
    protected void checkPage(CheckpointConfig.ExtractPoint extractPoint, Image image, String formTypeId) {
        if (!extractPoint.getPage().equals(image.getDocumentPage())) {
            throw new RuntimeException(String.format("材料[%s]字段[%s]已分类图片页码与checkpoint页码不匹配：image=%s, checkpoint=%s",
                    formTypeId, extractPoint.getDocumentField(), image.getDocumentPage(), extractPoint.getPage()));
        }
    }

    /**
     * 检查是否配置字段别名
     *
     * @param extractPoint {@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}
     * @param formTypeId   材料分类
     */
    protected void checkAlias(CheckpointConfig.ExtractPoint extractPoint, String formTypeId) {
        if (CollectionUtil.isEmpty(extractPoint.getAlias())) {
            throw new RuntimeException(String.format("材料[%s]字段[%s]提取点未配置字段别名",
                    formTypeId, extractPoint.getDocumentField()));
        }
    }
}
