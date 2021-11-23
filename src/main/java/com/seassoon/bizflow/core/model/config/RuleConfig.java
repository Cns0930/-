package com.seassoon.bizflow.core.model.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class RuleConfig {
    private Integer rulePkId;
    private Integer rowIndex;
    private String sid;
    private String sname;
    private String ruleId;
    @JsonProperty("rule_triger")
    private String ruleTrigger;
    private String ruleChange;
    private String rulePoint;
    private String ruleTips;
    private String ruleTipsForUser;
    private String ruleDesc;
    private String ruleInputs;
    private String ruleType;
    private String ruleArgs;
    private String ruleLaw;
    private String process;
    private String showField;
    private Integer action;
}
