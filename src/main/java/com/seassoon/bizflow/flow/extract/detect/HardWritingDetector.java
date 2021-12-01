package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手写签字{@link Detector}
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
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
        List<List<Integer>> detectArea = (List<List<Integer>>) params.get("detectArea");
        Double threshold = (Double) params.get("threshold");
        List<Item> hw = elements.getHw();

        List<Item> items = hw.stream().filter(item -> {
            List<List<Integer>> position = item.getPosition();
            Double overlapArea = getIOT(position, detectArea);
            return overlapArea > threshold;
        }).collect(Collectors.toList());

        if (CollectionUtil.isNotEmpty(items)) {
            List<List<Integer>> position = merge(items);
            List<List<Integer>> location = Arrays.asList(
                    Arrays.asList(position.get(0).get(1), position.get(0).get(0)),
                    Arrays.asList(position.get(1).get(1), position.get(1).get(0)));
            return Field.of("已签字", location, 1.0);
        } else {
            return Field.of("未签字", null, 1.0);
        }
    }
}
