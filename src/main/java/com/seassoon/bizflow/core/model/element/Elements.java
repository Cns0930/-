package com.seassoon.bizflow.core.model.element;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Elements {
    private List<Item> checkbox = new ArrayList<>();
    private List<Item> hw = new ArrayList<>();
    private List<Item> stamp = new ArrayList<>();
    @JsonProperty("idcard")
    private List<Item> idCard = new ArrayList<>();
    private List<Item> date = new ArrayList<>();
}
