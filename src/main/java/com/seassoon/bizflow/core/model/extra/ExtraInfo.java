package com.seassoon.bizflow.core.model.extra;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class ExtraInfo {
    @JsonProperty("ai_subkey_recognition_flag")
    private Integer aiSubKeyRecognitionFlag;
    @JsonProperty("subkey_set")
    private List<String> subKeySet = new ArrayList<>();
    private List<ExtraKVInfo> extraKvInfo = new ArrayList<>();
}
