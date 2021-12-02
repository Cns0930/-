package com.seassoon.bizflow.core.model.element;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档元素
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Item {
    private String label;
    private List<List<Integer>> position = new ArrayList<>();
    private Double score;

    public enum Label{
        handwriting, handwriting_sign,
        handwriting_date,
        stamp_circle, stamp_oval, stamp_red,
        sfzf, sfzb,
        checkbox
    }
}
