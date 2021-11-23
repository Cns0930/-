package com.seassoon.bizflow.flow;

import com.seassoon.bizflow.core.component.LocalStorage;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.flow.classify.DocClassify;
import com.seassoon.bizflow.flow.ocr.OCR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认预审流程处理器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class BizFlow extends AbstractFlow {

    private static final Logger logger = LoggerFactory.getLogger(BizFlow.class);

    @Autowired
    private LocalStorage localStorage;
    @Autowired
    private OCR ocr;
    @Autowired
    private DocClassify docClassify;

    @Override
    protected List<OcrOutput> ocr(List<Image> images) {
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start();

        // 处理图片OCR
        List<OcrOutput> ocrOutputs = ocr.ocr(images);

        // 保存本地JSON
        localStorage.save(ocrOutputs, "ocr_output.json");

        stopWatch.stop();
        logger.info("图片OCR处理完成，共耗时{}秒", stopWatch.getTotalTimeSeconds());
        return ocrOutputs;
    }

    @Override
    protected Map<String, List<Image>> sort(List<OcrOutput> ocrOutputs, Input input) {
        return docClassify.classify(ocrOutputs, input.getImageList(), input.getConfig().getSortConfig(), input.getExtraInfo())
                .stream().collect(Collectors.groupingBy(Image::getDocumentLabel));
    }
}
