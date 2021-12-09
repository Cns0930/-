package com.seassoon.bizflow.core.model.extra;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Field {
    private String fieldContent;
    private List<List<Integer>> fieldLocation = new ArrayList<>();
    private Double score = 1.0;

    public static Field of(String fieldContent, List<List<Integer>> fieldLocation, Double score) {
        Field field = new Field();
        field.setFieldContent(fieldContent);
        field.setFieldLocation(fieldLocation);
        field.setScore(score);
        return field;
    }
}
