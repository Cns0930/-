package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.util.TextUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 相似性匹配器，对应{@link SortConfig.TypeIdField}#method = similar
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class SimilarMatcher extends AbstractMatcher {

    private final Double threshold;

    public SimilarMatcher(Double threshold) {
        this.threshold = threshold;
    }

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return texts.stream().anyMatch(str -> {
            String content = ReUtil.replaceAll(str, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
            return patterns.stream().anyMatch(pattern -> TextUtils.similarity(pattern, content, threshold) > threshold);
        });
    }
}
