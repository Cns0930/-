package com.seassoon.bizflow.core.model.ocr;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class OcrOutput {
    private String imageName;
    private String corrUrl;
    private OcrResult ocrResult;
    private OcrResult ocrResultWithoutLineMerge;
}
