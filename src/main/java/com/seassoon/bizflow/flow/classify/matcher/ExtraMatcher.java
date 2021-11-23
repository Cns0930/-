package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.util.TextUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 结构化数据提取字段匹配器，对应{@link SortConfig.TypeIdField}#method = extra
 * @author lw900925 (liuwei@seassoon.com)
 */
public class ExtraMatcher extends AbstractMatcher {

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return texts.stream().anyMatch(str -> {
            String content = ReUtil.replaceAll(str, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
            return patterns.stream().anyMatch(pattern -> matchExtra(pattern, content));
        });
    }
}
