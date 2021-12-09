package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint.SignSealId.FILL;
import static com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint.SignSealId.HANDWRITING;

/**
 * 手写签字{@link Detector} / 是否已填写
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class HandwritingDetector extends DocElementDetector {

    // 提示语信息
    private Map<String, String[]> msgMap = new HashMap<String, String[]>() {{
        put(HANDWRITING.getValue(), new String[]{"已签字", "未签字"});
        put(FILL.getValue(), new String[]{"已填写", "未填写"});
    }};

    @SuppressWarnings("unchecked")
    @Override
    public Field detectField(Map<String, Object> params) {
        // 单张图片的检测结果
        Elements elements = (Elements) params.get("elements");
        String signSealId = (String) params.get("signSealId");
        if (elements == null || !msgMap.containsKey(signSealId)) {
            return Field.of(null, null, 0.0D);
        }

        // 检测区域的坐标
        List<List<Integer>> detectArea = (List<List<Integer>>) params.get("detectArea");
        Double threshold = (Double) params.get("threshold");
        List<Item> hw = elements.getHw();

        List<List<Integer>> location = detectLocation(hw, detectArea, threshold);
        if(location != null){
            return Field.of(msgMap.get(signSealId)[0], location, 1.0);
        }else {
            return Field.of(msgMap.get(signSealId)[1], null, 1.0);
        }
    }
}
