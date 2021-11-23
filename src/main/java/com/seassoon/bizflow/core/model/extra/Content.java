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
public class Content {
    private String imageId;
    private String documentField;
    private Integer page;
    private String isTrue;
    private String imageOrString;
    private List<FieldValueInfo> valueInfo = new ArrayList<>();
    private String source;
    private String sortProperty;
    private String displayProperty;
}
