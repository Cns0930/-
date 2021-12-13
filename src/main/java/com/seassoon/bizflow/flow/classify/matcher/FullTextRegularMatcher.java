package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 全文正则匹配器，对应{@link SortConfig.TypeIdField}#method = Regular
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class FullTextRegularMatcher extends AbstractMatcher {

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return matchRegular(patterns, String.join(StrUtil.EMPTY, texts));
    }
}
