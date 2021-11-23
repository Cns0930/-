package com.seassoon.bizflow.core.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seassoon.bizflow.core.model.Input;
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
public class SortConfig {
    private String sid;
    @JsonProperty("typeid2doc")
    private Map<String, String> typeIdDoc = new HashMap<>();
    @JsonProperty("typeid2field")
    private Map<String, List<List<TypeIdField>>> typeIdField = new HashMap<>();

    // ---------- TypeIdField ----------
    @Data
    @EqualsAndHashCode
    public static class TypeIdField {
        private String method;
        private List<String> pattern = new ArrayList<>();
        private List<String> except = new ArrayList<>();
        private Integer page;
        private Integer totalPages;
    }
}
