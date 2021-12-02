package com.seassoon.bizflow.flow.extract.resolve;

import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.ImgUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.springframework.beans.factory.annotation.Autowired;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractResolver implements Resolver {

    @Autowired
    private BizFlowProperties properties;

    @Override
    public Content resolve(Map<String, Object> params) {
        return null;
    }

    // 该方法由子类实现
    @Override
    public abstract boolean support(CheckpointConfig.ExtractPoint extractPoint);

    /**
     * 将一个{@link CheckpointConfig.ExtractPoint}对象转化为{@link Content}对象
     *
     * @param imageId      图片ID
     * @param extractPoint {@link CheckpointConfig.ExtractPoint}提取点信息
     * @return {@link Content}，value_info是null，需要提取计算后填入
     */
    protected Content mapToContent(String imageId, CheckpointConfig.ExtractPoint extractPoint) {
        Content content = new Content();
        content.setImageId(imageId);
        content.setDocumentField(extractPoint.getDocumentField());
        content.setPage(extractPoint.getPage());
        content.setImageOrString(Content.VALUE_TYPES.get(extractPoint.getValueType()));
        content.setSource("smj"); // 默认为扫描件
        content.setSortProperty(extractPoint.getSortProperty());
        content.setDisplayProperty(extractPoint.getDisplayProperty());
        return content;
    }

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
        String strFilename = y + "-" + x + "_" + location.get(1).get(0) + "-" + location.get(1).get(1);
        Path snapshot = Paths.get(properties.getLocalStorage(), recordId, "files/snapshot", image.getDocumentLabel(), image.getImageId(), strFilename);
        ImgUtils.cut(Paths.get(image.getCorrected().getLocalPath()).toFile(), snapshot.toFile(), rectangle);
        return snapshot;
    }
}
