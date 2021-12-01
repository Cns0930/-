package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手写签字{@link Detector} / 是否已填写
 * @author lw900925 (liuwei@seassoon.com)
 */
public class HardWritingDetector extends DocElementDetector {

    @SuppressWarnings("unchecked")
    @Override
    public Field detectField(Map<String, Object> params) {
        // 单张图片的检测结果
        Elements elements = (Elements) params.get("elements");
        if (elements == null) {
            return Field.of(null, null, 0);
        }

        // 检测区域的坐标
        List<List<Integer>> area = (List<List<Integer>>) params.get("area");
        Double threshold = (Double) params.get("threshold");
        List<Item> hw = elements.getHw();

        List<Item> items = hw.stream().filter(item -> {
            List<List<Integer>> position = item.getPosition();
            Double overlapArea = getIOT(position, area);
            return overlapArea > threshold;
        }).collect(Collectors.toList());

        String posCont = null;
        String negCont = null;
        CheckpointConfig.ExtractPoint.SignSealId signSealId =
                CheckpointConfig.ExtractPoint.SignSealId.getByValue((String) params.get("signSealId"));
        if (signSealId != null) {
            switch (signSealId) {
                case HANDWRITING:
                    posCont = "已签字";
                    negCont = "未签字";
                    break;
                case FILL:
                    posCont = "已填写";
                    negCont = "未填写";
                    break;
            }
        }

        if (CollectionUtil.isNotEmpty(items)) {
            List<List<Integer>> position = merge(items);
            List<List<Integer>> location = Arrays.asList(
                    Arrays.asList(position.get(0).get(1), position.get(0).get(0)),
                    Arrays.asList(position.get(1).get(1), position.get(1).get(0)));
            return Field.of(posCont, location, 1.0);
        } else {
            return Field.of(negCont, null, 1.0);
        }
    }
}
