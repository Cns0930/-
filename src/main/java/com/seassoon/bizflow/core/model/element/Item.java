package com.seassoon.bizflow.core.model.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 文档元素
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Item {
    private String label;
    private List<List<Integer>> position;
    private Double score;
}
