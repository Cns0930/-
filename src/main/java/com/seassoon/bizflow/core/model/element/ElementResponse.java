package com.seassoon.bizflow.core.model.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class ElementResponse {
    private Double timeCost;
    private Map<String, Elements> elements = new HashMap<>();
    private String status;
    private String message;
}
