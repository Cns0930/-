package com.seassoon.bizflow.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.project.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Input {
    private String version;
    private String sid;
    private String projectId;
    private String recordId;
    private Integer calcMode;
    private List<Image> imageList = new ArrayList<>();
    private ExtraInfo extraInfo;
    private Project projectInfo;
    private Integer approvalStage;
    private Api apiInfo;
    private Config config;

    // ---------- Api ----------
    @Data
    @EqualsAndHashCode
    public static class Api {
        private List<String> ocrOutput;
        private List<String> sortOutput;
    }

    // ---------- Config ----------
    @Data
    @EqualsAndHashCode
    public static class Config {

        @JsonProperty("checkpointConfig")
        private List<CheckpointConfig> checkpointConfig = new ArrayList<>();
        @JsonProperty("sortConfig")
        private SortConfig sortConfig;
        @JsonProperty("mappingConfig")
        private Map<String, String> mappingConfig = new HashMap<>();
        @JsonProperty("subkeyConfig")
        private List<SubKeyConfig> subKeyConfig = new ArrayList<>();
        @JsonProperty("ruleConfig")
        private List<RuleConfig> ruleConfig = new ArrayList<>();
        @JsonProperty("priorityConfig")
        private Map<String, Object> priorityConfig = new HashMap<>();

        // ---------- CheckpointConfig ----------
        @Data
        @EqualsAndHashCode
        public static class CheckpointConfig {
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

        // ---------- SubKeyConfig ----------
        @Data
        @EqualsAndHashCode
        public static class SubKeyConfig {
            private String sid;
            @JsonProperty("subkey_name")
            private Map<String, SubKeyProperty> subKeyName = new HashMap<>();

            // ---------- SubKeyProperty ----------
            @Data
            @EqualsAndHashCode
            public static class SubKeyProperty {
                private String additionalParameter;
                private String input;
                private String judgmentWay;
            }
        }

        // ---------- RuleConfig ----------
        @Data
        @EqualsAndHashCode
        public static class RuleConfig {
            private Integer rulePkId;
            private Integer rowIndex;
            private String sid;
            private String sname;
            private String ruleId;
            @JsonProperty("rule_triger")
            private String ruleTrigger;
            private String ruleChange;
            private String rulePoint;
            private String ruleTips;
            private String ruleTipsForUser;
            private String ruleDesc;
            private String ruleInputs;
            private String ruleType;
            private String ruleArgs;
            private String ruleLaw;
            private String process;
            private String showField;
            private Integer action;
        }
    }
}
