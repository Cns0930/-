package com.seassoon.bizflow.core.model.ocr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class OcrResult {
    private String errorMsg;
    private List<Block> blocks = new ArrayList<>();
    private String imageName;
    private String imageExtension;
}
