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
public class ExtraKVInfo {
    private String source;
    private Integer useFlag;
    private List<Document> documentList = new ArrayList<>();
}
