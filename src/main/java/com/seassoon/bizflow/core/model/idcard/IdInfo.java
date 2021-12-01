package com.seassoon.bizflow.core.model.idcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class IdInfo {
    private String field;
    private String text;
    private Double textScore;
    private List<List<Integer>> position = new ArrayList<>();
}
