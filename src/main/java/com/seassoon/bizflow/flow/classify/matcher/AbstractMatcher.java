package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.util.TextUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
                    String match = ReUtil.get(Pattern.compile("^\\{.*}$"), except, 0);
                    if (StrUtil.isNotBlank(match)) {
                        match = match.replace("{", StrUtil.EMPTY).replace("}", StrUtil.EMPTY);
                        if (StrUtil.isNotBlank(match)) {
                            // 用正则匹配一次
                            String content = ReUtil.replaceAll(str, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
                            isMatch = StringUtils.isNotEmpty(content) && matchContext(match, content) != null;
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
            expect = ReUtil.replaceAll(expect, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
            String keyword = expect.replace("x_", StrUtil.EMPTY);
            return texts.stream().anyMatch(str -> matchExtra(keyword, str));
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

    /**
     * 在文本中按正则表达式提取
     * <p>
     * 正则模式：regex = '(.*)公司'，content：'上海思贤有限公司'
     *
     * @param regex   正则表达式
     * @param content 文本内容
     * @return 提取结果，如果有返回第一个对应的内容，否则返回None
     */
    public static String matchContext(String regex, String content) {
        // check argument
        Assert.notBlank(regex, "Argument 'regex' cannot be null or blank.");

        // 按pattern中包含的匹配次数匹配
        if (regex.length() > 2) {
            String strNumber = ReUtil.get(Pattern.compile("#[#0-9]$"), regex, 0);
            // pattern末尾包含#
            if (StrUtil.isNotBlank(strNumber)) {
                regex = regex.substring(0, regex.indexOf("#"));
                //结尾有数字取对应数字 没数字取第一个匹配的值
                if (ReUtil.isMatch(Pattern.compile(".*\\d+.*"), strNumber)) {
                    strNumber = ReUtil.replaceAll(strNumber, Pattern.compile("[^0-9]"), "");
                    return TextUtils.regexMatch(regex, content, Integer.parseInt(strNumber));
                }
                return TextUtils.regexMatch(regex, content, 1);
            }
        }

        // 直接匹配pattern与文本内容，取匹配结果最长的字符串
        List<String> groups = ReUtil.getAllGroups(Pattern.compile(regex), content);
        return groups.stream().max(Comparator.comparingInt(String::length)).orElse(null);
    }

    /**
     * 在文本中按结构化数据提取值匹配
     *
     * @param keyword 提取关键字
     * @param content 文本内容
     * @return 匹配结果
     */
    public static boolean matchExtra(String keyword, String content) {
        Assert.notBlank(keyword, "Argument 'keyword' cannot be null or blank.");
        if (StrUtil.isBlank(content)) {
            return false;
        }

        // bug-3344，根据关键字长度动态调整相似度阈值
        double threshold = 0.00;
        if (keyword.length() <= 21) {
            // 抛物线的对称轴
            threshold = -0.0006 * Math.pow(keyword.length(), 2) + 0.025 * keyword.length() + 0.6736;
        } else {
            // 抛物线的最大值
            threshold = 0.93;
        }

        // 计算相似度
        if (content.length() < keyword.length()) {
            double score = TextUtils.jaroDistance(keyword, content);
            if (score > threshold) {
                return true;
            }
        }

        boolean isMatch = false;
        // 说实话下面这一段代码我也不知道什么意思
        long[] keywordSize = {
                Math.round(keyword.length() * 0.75),
                Math.min(Math.round(keyword.length() * 1.25), content.length())
        };
        for (long size : keywordSize) {
            long count = content.length() - size + 1;
            if (count > 0) {
                Double score = Stream.iterate(0, n -> n + 1)
                        .limit(count)
                        .map(i -> content.substring(i, i + Long.valueOf(size).intValue()))
                        .map(str -> TextUtils.jaroDistance(keyword, str))
                        .max(Double::compareTo).orElse(0.00);
                if (score > threshold) {
                    isMatch = true;
                    break;
                }
            }
        }
        return isMatch;
    }
}
