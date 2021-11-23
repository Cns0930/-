package com.seassoon.bizflow.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.config.SubKeyConfig;
import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.project.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    }
}
