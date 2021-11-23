package com.seassoon.bizflow.core.model.rule;

import com.seassoon.bizflow.core.model.extra.Field;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class ApprovalDoc {
    private String imageId;
    private String documentLabel;
    private String documentField;
    private List<Field> ruleFilterValueInfo = new ArrayList<>();
    private String source;
}
