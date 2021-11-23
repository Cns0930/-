package com.seassoon.bizflow.core.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.core.model.Input;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class SubKeyConfig {
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
