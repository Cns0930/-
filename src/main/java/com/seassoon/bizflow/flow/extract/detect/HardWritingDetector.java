package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 手写签字{@link Detector} / 是否已填写
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

        List<List<Integer>> location = detectLocation(hw, detectArea, threshold);
        if(location != null){
            return Field.of(posCont, location, 1.0);
        }else {
            return Field.of(negCont, null, 1.0);
        }
    }
}
