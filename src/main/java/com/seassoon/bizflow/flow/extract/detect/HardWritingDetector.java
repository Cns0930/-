package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.core.model.extra.Field;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手写签字{@link Detector}
 * @author lw900925 (liuwei@seassoon.com)
 */
public class HardWritingDetector extends DocElementDetector {

    @SuppressWarnings("unchecked")
    @Override
    public Field detectField(Map<String, Object> params) {

        // 单张图片的检测结果
        Map<String, Object> elements = (Map<String, Object>) params.get("elements");
        if (elements == null) {
            return Field.of(null, null, 0);
        }

        // 检测区域的坐标
        List<List<Integer>> area = (List<List<Integer>>) params.get("area");
        Double threshold = (Double) params.get("threshold");
        List<Map<String, Object>> hw = (List<Map<String, Object>>) elements.get("hw");

        List<Map<String, Object>> matches = hw.stream().filter(map -> {
            List<List<Integer>> targetPos = (List<List<Integer>>) map.get("position");
            Double overlapArea = getIOT(targetPos, area);
            return overlapArea > threshold;
        }).collect(Collectors.toList());

        if (CollectionUtil.isNotEmpty(matches)) {
            List<List<Integer>> position = merge(matches);
            List<List<Integer>> location = Arrays.asList(
                    Arrays.asList(position.get(0).get(1), position.get(0).get(0)),
                    Arrays.asList(position.get(1).get(1), position.get(1).get(0)));
            return Field.of("已签字", location, 1.0);
        } else {
            return Field.of("未签字", null, 1.0);
        }
    }
}
