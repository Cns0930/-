package com.seassoon.bizflow.core.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.flow.extract.detect.*;
import javafx.scene.control.CheckBox;
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

        public enum SignSealId {
            HANDWRITING("1", HandwritingDetector.class),
            FILL_DATE("3", StampDetector.class),
            ATTACH_ID("4", AttachDetector.class),
            FILL("7", HandwritingDetector.class),

            CHECKBOX("13", CheckboxDetector.class),
            STAMP_NEW("14", StampDetector.class),
            CHECK_RIGHT("15_right", CheckboxDetector.class),
            CHECK_LEFT("15_left", CheckboxDetector.class),
            ATTACH_PHOTO("16", AttachDetector.class),
            STAMP_RED("17", StampDetector.class),

            ;
            private String value;
            private Class<? extends Detector> detector;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }

            public Class<? extends Detector> getDetector() {
                return detector;
            }

            public void setDetector(Class<? extends Detector> detector) {
                this.detector = detector;
            }

            SignSealId(String value, Class<? extends Detector> detector) {
                this.value = value;
                this.detector = detector;
            }

            public static SignSealId getByValue(String value){
                for(SignSealId signSealId : values()){
                    if(signSealId.getValue().equals(value)){
                        return signSealId;
                    }
                }
                return null;
            }
        }
    }
}
