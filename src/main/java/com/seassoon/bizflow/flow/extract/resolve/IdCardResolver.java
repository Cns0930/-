package com.seassoon.bizflow.flow.extract.resolve;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableMap;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.idcard.IdCard;
import com.seassoon.bizflow.core.model.idcard.IdCardResponse;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.ImgUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * 身份证信息提取
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class IdCardResolver extends AbstractResolver {

    private static final Logger logger = LoggerFactory.getLogger(IdCardResolver.class);

    /**
     * 身份证提取结果缓存
     */
    private final LRUCache<String, List<IdCard>> ID_CARD_CACHE = new LRUCache<>(10);

    /**
     * 身份证提取字段与signSealId映射
     */
    private final Map<String, String> FIELD_SEAL_ID_MAP = new HashMap<String, String>() {{
        put("姓名", "5");
        put("有效期限", "6");
        put("住址", "8");
        put("公民身份号码", "9");
        put("性别", "10");
        put("民族", "11");
        put("出生", "12");
        put("签发机关", "20");
    }};

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private HTTPCaller httpCaller;

    @Override
    public Content resolve(Map<String, Object> params) {
        Image image = (Image) params.get("image");
        CheckpointConfig.ExtractPoint extractPoint = (CheckpointConfig.ExtractPoint) params.get("extractPoint");
        String signSealId = extractPoint.getSignSealId();
        String strPath = image.getClassifiedPath();

        // 返回的结果
        Content content = Content.of(image.getImageId(), extractPoint);

        // 计算截图区域并切图
        ImgUtils.Shape shape = ImgUtils.getShape(strPath);
        List<List<Integer>> location = ImgUtils.calcLocation(shape, extractPoint.getInitPosition());
        Path snapshot = snapshot(image, location);

        // 获取身份证提取结果
        String url = properties.getIntegration().get(BizFlowProperties.Service.ID_CARD_EXTRACT);
        List<IdCard> idCards = getIdCard(url, image.getImageId(), snapshot);
        if (CollectionUtil.isEmpty(idCards)) {
            logger.error("获取身份证提取信息出错，结果为null");
            return content;
        }

        // 匹配身份证提取的内容
        Field field = idCards.stream().flatMap(idCard -> idCard.getInfo().stream())
                .filter(info -> signSealId.equals(FIELD_SEAL_ID_MAP.get(info.getField())))
                .map(info -> {
                    List<List<Integer>> position = info.getPosition();
                    List<List<Integer>> fieldLocation = Arrays.asList(
                            Arrays.asList(position.get(0).get(1), position.get(0).get(0)),
                            Arrays.asList(position.get(1).get(1), position.get(1).get(0)));
                    return Field.of(info.getText(), fieldLocation, info.getTextScore());
                }).findAny().orElse(null);

        // 对location修正
        if (field != null) {
            List<List<Integer>> fieldLocation = field.getFieldLocation();
            int xMin = fieldLocation.get(0).get(0) + location.get(0).get(0),
                    yMin = fieldLocation.get(0).get(1) + location.get(0).get(1),
                    xMax = fieldLocation.get(1).get(0) + location.get(0).get(0),
                    yMax = fieldLocation.get(1).get(1) + location.get(0).get(1);
            field.setFieldLocation(Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax)));

            // 将field填入content
            content.setValueInfo(Collections.singletonList(field));
        }
        return content;
    }

    @Override
    public boolean support(CheckpointConfig.ExtractPoint extractPoint) {
        String signSealId = extractPoint.getSignSealId();
        if (StrUtil.isBlank(signSealId)) {
            return false;
        }
        return FIELD_SEAL_ID_MAP.containsValue(signSealId);
    }

    /**
     * 获取身份证提取信息。<p>
     * 先从缓存中获取，如果缓存中没有就调用接口获取。
     *
     * @param url     请求URL
     * @param imageId 图片ID
     * @param file    图片本地路径
     * @return 提取结果
     */
    private List<IdCard> getIdCard(String url, String imageId, Path file) {
        List<IdCard> idCards = ID_CARD_CACHE.get(imageId);
        if (idCards == null) {
            idCards = callAPI(url, file);
            ID_CARD_CACHE.put(imageId, idCards);
        }
        return idCards;
    }

    private List<IdCard> callAPI(String url, Path file) {
        // 要上传的图片
        Map<String, Object> params = ImmutableMap.<String, Object>builder()
                .put("file", new FileSystemResource(file))
                .build();
        IdCardResponse response = httpCaller.post(url, params, MediaType.MULTIPART_FORM_DATA, IdCardResponse.class);
        if (response == null) {
            logger.error("调用身份证提取API出错，结果为null");
            return null;
        }
        return response.getBlocks();
    }
}
