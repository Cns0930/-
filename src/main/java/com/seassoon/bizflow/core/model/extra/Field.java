package com.seassoon.bizflow.core.model.extra;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Field {
    private String fieldContent;
    private List<List<String>> fieldLocation;
    private Double score;

    public static Field of(String fieldContent, List<List<String>> fieldLocation, double score) {
        Field field = new Field();
        field.setFieldContent(fieldContent);
        field.setFieldLocation(fieldLocation);
        field.setScore(score);
        return field;
    }
}
