package com.seassoon.bizflow.flow.rule;

import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.rule.Approval;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 临时方案：调用老版本python的接口处理规则
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DummyRuleEngine implements RuleEngine {
    @Override
    public List<Approval> process(List<DocumentKV> docKVs, List<RuleConfig> rules) {
        return null;
    }
}
