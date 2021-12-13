package com.seassoon.bizflow.core.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Sets;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public class ReUtils extends ReUtil {

    /**
     * 匹配正则并提取内容
     * <p>
     * 正则模式：regex = '(.*)公司'，content：'上海思贤有限公司'
     *
     * @param regex   正则表达式
     * @param content 文本内容
     * @return 提取结果，如果有返回第一个对应的内容，否则返回None
     */
    public static String reExtract(String regex, String content) {
        // check argument
        Assert.notBlank(regex, "Argument 'regex' cannot be null or blank.");

        // 按pattern中包含的匹配次数匹配
        if (regex.length() > 2) {
            String strNumber = get(Pattern.compile("#[#0-9]$"), regex, 0);
            // pattern末尾包含#
            if (StrUtil.isNotBlank(strNumber)) {
                regex = regex.substring(0, regex.indexOf("#"));
                //结尾有数字取对应数字 没数字取第一个匹配的值
                if (isMatch(Pattern.compile(".*\\d+.*"), strNumber)) {
                    strNumber = replaceAll(strNumber, Pattern.compile("[^0-9]"), "");
                    return reMatch(regex, content, Integer.parseInt(strNumber));
                }
                return reMatch(regex, content, 1);
            }
        }

        // 直接匹配pattern与文本内容，取匹配结果最长的字符串
        List<String> groups = getAllGroups(Pattern.compile(regex), content);
        return groups.stream().max(Comparator.comparingInt(String::length)).orElse(null);
    }

    /**
     * 正则匹配并提取文本
     *
     * @param regex  正则表达式
     * @param input  输入内容
     * @param number 文本匹配次数
     * @return 匹配到的文本
     */
    public static String reMatch(String regex, String input, Integer number) {
        // check arguments
        Assert.notEmpty(regex, "Argument 'regex' cannot be null or empty.");
        Assert.notEmpty(input, "Argument 'input' cannot be null or empty.");
        Assert.notNull(number, "Argument 'number' cannot be null.");

        // 正则表达式中输入的'#汉'替换为中文正则表达式部分
        regex = StrUtil.replace(regex, "#汉", "[\u4e00-\u9fa5]");

        // 去除一些无效字符
        input = ReUtil.replaceAll(input, "[:：，,。()《》<>（）?？!！/|]", "");
        input = ReUtil.replaceAll(input, "[\\s]+", " ");
        input = ReUtil.replaceAll(input, "([\u4e00-\u9fa5])[\\s\\._-]+([\u4e00-\u9fa5])", "$1$2");
        input = ReUtil.replaceAll(input, "([\u4e00-\u9fa5])[\\s\\._-]+([\u4e00-\u9fa5])", "$1$2");

        // 如果匹配成功，就返回结果
        List<String> groups = ReUtil.getAllGroups(Pattern.compile(regex), input);
        if (CollectionUtil.isNotEmpty(groups)) {
            return CollectionUtil.getLast(groups).trim();
        }

        number = number < 3 ? 5 : number;
        String strPattern = String.format("[\u4e00-\u9fa5]{%s,}", number);
        groups = ReUtil.getAllGroups(Pattern.compile(strPattern), regex);
        for (String group : groups) {
            // 取倍数，字段的数字超过##的字段多少倍
            int count = group.length() / number;
            count = Math.min(count, 2);

            // 当出现count = 3时，返回类似[(0,1,2),(0,1,3)....]的列表
            Set<Integer> set = Stream.iterate(0, n -> n + 1).limit(group.length()).collect(Collectors.toSet());
            @SuppressWarnings("UnstableApiUsage")
            Set<Set<Integer>> pos = Sets.combinations(set, count);

            // 包括相似字的一个列表
            for (Set<Integer> tuple : pos) {
                String[] chars = group.split("");
                tuple.forEach(index -> {
                    // 少字换字或者多字
                    chars[index] = "[0-9a-zA-Z_\u4e00-\u9fa5]{0,2}";
                });

                // 随机换一个字
                String pattern = ReUtil.replaceFirst(Pattern.compile(group), regex, String.join("", chars));

                // 完成##操作之后再去匹配正则
                List<String> mGroups = ReUtil.getAllGroups(Pattern.compile(pattern), input);
                if (CollectionUtil.isNotEmpty(mGroups)) {
                    return CollectionUtil.getLast(mGroups).trim();
                }
            }
        }
        return null;
    }

    /**
     * 按文本相似度匹配
     *
     * @param keyword 提取关键字
     * @param content 文本内容
     * @return 匹配结果
     */
    public static boolean simiMatch(String keyword, String content) {
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
