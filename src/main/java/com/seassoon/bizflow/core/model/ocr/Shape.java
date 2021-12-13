package com.seassoon.bizflow.core.model.ocr;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Shape {
    private Integer width;
    private Integer height;

    public static Shape of(Integer width, Integer height) {
        Shape shape = new Shape();
        shape.setWidth(width);
        shape.setHeight(height);
        return shape;
    }
}
