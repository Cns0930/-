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
public class Block {
    private List<Position> position = new ArrayList<>();
    private Double score = 0D;
    private String text;
    private List<Character> characters = new ArrayList<>();
}