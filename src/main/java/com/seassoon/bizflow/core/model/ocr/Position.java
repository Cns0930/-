package com.seassoon.bizflow.core.model.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class Position {
    private Integer x;
    private Integer y;

    public static Position of(Integer x, Integer y) {
        return new Position(x, y);
    }
}
