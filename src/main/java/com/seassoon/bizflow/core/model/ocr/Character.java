package com.seassoon.bizflow.core.model.ocr;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Character {
    private List<Position> position = new ArrayList<>();
    private Double score = 0D;
    private String text;
}
