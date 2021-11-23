package com.seassoon.bizflow.flow.rule;

import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.rule.Approval;

import java.util.List;

/**
 * 规则引擎
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface RuleEngine {

    /**
     * 处理规则
     *
     * @param docKVs      文档结构化数据
     * @param ruleConfigs 事项配置-规则
     * @return 规则审批结果
     */
    List<Approval> process(List<DocumentKV> docKVs, List<RuleConfig> ruleConfigs);
}
