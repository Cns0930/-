package com.seassoon.bizflow.core.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.core.model.Input;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class CheckpointConfig {
    @JsonProperty("form_typeid")
    private String formTypeId;
    private Boolean multiPage;
    private List<ExtractPoint> extractPoint = new ArrayList<>();

    // ---------- ExtractPoint ----------
    @Data
    @EqualsAndHashCode
    public static class ExtractPoint {
        private String documentField;
        private List<String> alias = new ArrayList<>();
        private Integer page;
        private String sortProperty;
        private String displayProperty;
        private String valueType;
        private String valueEnvironment;
        private String keyValueRelativePosition;
        private String lineMerge;
        private List<String> valuePattern = new ArrayList<>();
        private List<CutImgTag> cutImgTag = new ArrayList<>();
        private List<List<BigDecimal>> initPosition = new ArrayList<>();
        private String textStringPatternRange;
        private List<String> valueField = new ArrayList<>();
        private String valueProperty;
        private String signSealId;
        private String source;
        private Integer action;
        private Object value;

        // ---------- CutImgTag ----------
        @Data
        @EqualsAndHashCode
        public static class CutImgTag {
            private String pattern;
            private String method;
        }
    }
}
