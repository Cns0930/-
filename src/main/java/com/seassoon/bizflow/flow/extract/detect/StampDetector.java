package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Block;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.OcrResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 日期检测/是否盖章/是否盖红章
 * @author chimney
 * @date 2021/12/1
 */
@Component
public class StampDetector extends DocElementDetector{

    @Override
    public Field detectField(Map<String, Object> params) {
        CheckpointConfig.ExtractPoint.SignSealId signSealId =
                CheckpointConfig.ExtractPoint.SignSealId.getByValue((String) params.get("signSealId"));
        if (signSealId != null) {
            switch (signSealId) {
                case FILL_DATE:
                    return detectDate(params);
                case STAMP_NEW:
                    return detectStamp(params);
                case STAMP_RED:
                    return detectRedStamp(params);
            }
        }
        // fixme
        return Field.of(null, null, 0);
    }

    /**
     * 日期检测
     * @param params
     * @return
     */
    private Field detectDate(Map<String, Object> params) {
        // ocr检测日期
        OcrOutput ocrOutput = (OcrOutput) params.get("ocr");
        OcrResult ocrResult = ocrOutput.getOcrResultWithoutLineMerge();
        // whether_have_date_ocr
        String path = (String) params.get("path");
        ocrResult = getBlockOcr(ocrResult, path);
        Pattern pattern = Pattern.compile("[0-9]{4}年[0-9]{1,2}月[0-9]{1,2}日");
        List<Block> blockList = ocrResult.getBlocks();
        for(Block block : blockList){
            String text = block.getText();
            if(pattern.matcher(text).find()){
                return Field.of("已填写文件日期", Arrays.asList(
                        Arrays.asList(block.getPosition().get(0).getY(), block.getPosition().get(0).getX()),
                        Arrays.asList(block.getPosition().get(2).getY(), block.getPosition().get(2).getX())
                ), 1.0);
            }
        }

        // ocr没有得到的 用元素检测结果
        Elements elements = (Elements) params.get("elements");
        if (elements != null) {
            // 检测区域的坐标
            List<List<Integer>> detectArea = (List<List<Integer>>) params.get("detectArea");
            Double threshold = (Double) params.get("threshold");
            List<Item> date = elements.getDate();
            List<List<Integer>> locationDate = detectLocation(date, detectArea, threshold);
            if(locationDate != null){
                return Field.of("已填写文件日期", null, 1.0);
            }
        }

        // 无结果 看看是不是盖章
        // 如果是盖章则 则不知可否
        List<List<Integer>> locationStamp = detectStampMatch(params);
        if(locationStamp != null){
            return Field.of("是否填写文件日期", null, 0.3);
        }
        return Field.of("未填写文件日期", null, 1.0);
    }

    /**
     * 是否盖章
     * @param params
     * @return
     */
    private Field detectStamp(Map<String, Object> params){
        List<List<Integer>> location = detectStampMatch(params);
        if(location != null){
            return Field.of("已盖章", location, 1.0);
        }else {
            return Field.of("未盖章", null, 1.0);
        }
    }

    /**
     * 是否盖红章
     * @param params
     * @return
     */
    private Field detectRedStamp(Map<String, Object> params){
        // TODO
        return null;
    }

    private List<List<Integer>> detectStampMatch(Map<String, Object> params) {
        Elements elements = (Elements) params.get("elements");
        if (elements == null) {
            return null;
        }
        // 检测区域的坐标
        List<List<Integer>> detectArea = (List<List<Integer>>) params.get("detectArea");
        Double threshold = (Double) params.get("threshold");
        List<Item> stamp = elements.getStamp();
        return detectLocation(stamp, detectArea, threshold);
    }

}
