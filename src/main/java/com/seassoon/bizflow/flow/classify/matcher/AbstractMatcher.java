package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.util.ReUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * {@link Matcher}接口的抽象类，提供通用实现方法{@link AbstractMatcher#matchPattern}
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractMatcher implements Matcher {

    protected static final String IGNORE_PATTERN = "[A-Z a-z,，【】（）()\\[\\]；;。.、：: \\s]";

    @Override
    public boolean match(SortConfig.TypeIdField typeIdField, List<String> texts) {
        // 只要找到了except_value中{}内的字符，整个文档不参加分类
        boolean isExpect = texts.stream().anyMatch(str -> {
            if (StrUtil.isNotBlank(str) && CollectionUtil.isNotEmpty(typeIdField.getExcept())) {
                return typeIdField.getExcept().stream().anyMatch(except -> {
                    boolean isMatch = false;
                    // 匹配所有{}包裹的字符串
                    String match = ReUtils.get(Pattern.compile("^\\{.*}$"), except, 0);
                    if (StrUtil.isNotBlank(match)) {
                        match = match.replace("{", StrUtil.EMPTY).replace("}", StrUtil.EMPTY);
                        if (StrUtil.isNotBlank(match)) {
                            // 用正则匹配一次
                            String content = ReUtils.replaceAll(str, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
                            isMatch = StrUtil.isNotEmpty(content) && ReUtils.reExtract(match, content) != null;
                        }
                    }
                    return isMatch;
                });
            } else {
                return false;
            }
        });
        if (isExpect) {
            return false;
        }

        // 解析从结构化数据中取得的排除字段
        isExpect = typeIdField.getExcept().stream().filter(expect -> expect.startsWith("x_")).anyMatch(expect -> {
            expect = ReUtils.replaceAll(expect, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
            String keyword = expect.replace("x_", StrUtil.EMPTY);
            return texts.stream().anyMatch(str -> ReUtils.simiMatch(keyword, str));
        });
        if (isExpect) {
            return false;
        }

        return matchPattern(typeIdField.getPattern(), texts);
    }

    /**
     * 根据pattern字段匹配文本行内容
     *
     * @param patterns {@link SortConfig.TypeIdField} pattern字段值
     * @param texts    文本行
     * @return 匹配结果
     */
    protected abstract boolean matchPattern(List<String> patterns, List<String> texts);

    protected boolean matchRegular(List<String> patterns, String text) {
        // 替换掉特殊字符
        String content = ReUtils.replaceAll(text, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
        if (StrUtil.isNotBlank(text) && CollectionUtil.isNotEmpty(patterns)) {
            return patterns.stream().anyMatch(pattern -> StrUtil.isNotEmpty(content) && ReUtils.reExtract(pattern, content) != null);
        } else {
            return false;
        }
    }
}
