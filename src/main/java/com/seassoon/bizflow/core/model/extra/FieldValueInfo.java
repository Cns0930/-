package com.seassoon.bizflow.core.model.extra;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class FieldValueInfo {
    private String fieldContent;
    private List<List<String>> fieldLocation;
    private Double score;
}
