package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 行正则匹配器，对应{@link SortConfig.TypeIdField}#method = regular
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class RegularMatcher extends AbstractMatcher {

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return texts.stream().anyMatch(str -> matchRegular(patterns, str));
    }

    protected boolean matchRegular(List<String> patterns, String text) {
        // 替换掉特殊字符
        String content = ReUtil.replaceAll(text, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
        if (StrUtil.isNotBlank(text) && CollectionUtil.isNotEmpty(patterns)) {
            return patterns.stream().anyMatch(pattern -> matchContext(pattern, content) != null);
        } else {
            return false;
        }
    }
}
