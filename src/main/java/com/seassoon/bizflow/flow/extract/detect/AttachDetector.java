package com.seassoon.bizflow.flow.extract.detect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.seassoon.bizflow.core.component.ocr.OcrProcessor;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Block;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.OcrResult;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.flow.ocr.DocOCR;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 是否粘贴身份证/证件照片检测
 * @author chimney
 * @date 2021/11/30
 */
@Component
public class AttachDetector extends DocElementDetector{

    @Resource
    DocOCR docOCR;

    final private String[] idField = new String[]{"中华人民共和国","中华人民共和", "人民共和国", "共和国", "居民身份证", "居民身份",
            "公民身份号码", "签发机关", "签发", "有效期限", "日本国", "PASSPORT", "JAPAN","男","女","村"};

    @Override
    public Field detectField(Map<String, Object> params) {
        CheckpointConfig.ExtractPoint.SignSealId signSealId =
                CheckpointConfig.ExtractPoint.SignSealId.getByValue((String) params.get("signSealId"));
        if (signSealId != null) {
            switch (signSealId) {
                case ATTACH_ID:
                    return detectAttachID(params);

                case ATTACH_PHOTO:
                    return detectAttachPhoto(params);
            }
        }
        // fixme
        return Field.of(null, null, 0.0D);
    }

    /**
     * 是否粘贴身份证
     * @param params
     * @return
     */
    private Field detectAttachID(Map<String, Object> params) {
        // 原为重新调ocr 根据算法反馈 改为用ocr剪裁的方式
        OcrResult ocrResult = getBlockOcr(((OcrOutput) params.get("ocr")).getOcrResultWithoutLineMerge(), (String) params.get("path"));

        // 排序
        ocrResult = docOCR.sortBlock(ocrResult);
        // ocr结果关键词判断
        StringBuffer allContext = new StringBuffer();
        ocrResult.getBlocks().forEach(block -> {
            allContext.append(block.getText());
        });
        if (StringUtils.containsAny(allContext.toString(), idField)) {
            return Field.of("已粘贴身份证件", null, 1.0);
            }

            // 再用元素判断
            Elements elements = (Elements) params.get("elements");
            if (Objects.nonNull(elements)) {
                // 取裁剪区域和阈值
                List<List<Integer>> area = (List<List<Integer>>) params.get("area");
                Double threshold = (Double) params.get("threshold");

                if (elements.getIdCard().stream().anyMatch(each -> getIOT(each.getPosition(), area) > threshold)) {
                    return Field.of("已粘贴身份证件", null, 1.0);
                }
            }
//        }

        return Field.of("未粘贴身份证件", null, 1.0);
    }

    /**
     * 是否粘贴照片
     * @param params
     * @return
     */
    private Field detectAttachPhoto(Map<String, Object> params){


        // todo 需要调用 opencv 待完成

//        // 读取照片
//        Mat org = Imgcodecs.imread((String )params.get("path"));
//        // 转换灰色
//        Mat des = new Mat();
//        Imgproc.cvtColor(org, des, Imgproc.COLOR_BGR2GRAY);
//        // 人脸识别分类器
//

        return Field.of("未粘贴", null, 1.0);
    }
}
