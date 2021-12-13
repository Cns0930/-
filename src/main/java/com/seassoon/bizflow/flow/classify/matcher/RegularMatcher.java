package com.seassoon.bizflow.flow.classify.matcher;

import com.seassoon.bizflow.core.model.config.SortConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 行正则匹配器，对应{@link SortConfig.TypeIdField}#method = regular
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class RegularMatcher extends AbstractMatcher {

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return texts.stream().anyMatch(str -> matchRegular(patterns, str));
    }
}
