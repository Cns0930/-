package com.seassoon.bizflow.core.model.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 规则处理结果
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Approval {
    private String approvalPointId;
    private String approvalPoint;
    private String approvalBasis;
    private String approvalMessage;
    private List<String> lawBasis = new ArrayList<>();
    private String approvalResult;
    private List<ApprovalDoc> documentAndField = new ArrayList<>();
    private String process;
}
