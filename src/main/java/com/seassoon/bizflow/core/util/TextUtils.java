package com.seassoon.bizflow.core.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文本字符工具类
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class TextUtils {

    private TextUtils() {
        // do not initialize
    }

    /**
     * 求两个字符串的相似度，基于jaro similarity算法实现。<br/>
     * <p>
     * <ol>
     *   <li>“指定代表或者共同委托代理人授权委托书”与“指定代表人或者共同委托代理人授权委托书”相似度为0.89，不符合业务场景，
     *   引入正序和倒序求相似，并求最大值，得出相似度为0.988</li>
     *   <li>在1基础上“住所口”与“住所”相似度为0.88，不符合业务场景，引入一个与字符串长度相关的函数，对短字符串相似系数适当放大，得出相似度为0.929</li>
     * </ol>
     *
     * @param a         字符串a
     * @param b         字符串b
     * @param threshold 相似度阈值
     * @return 相似度
     */
    public static double similarity(String a, String b, double threshold) {
        Assert.notBlank(a, "Argument 'a' cannot be null");
        Assert.notBlank(b, "Argument 'b' cannot be null");

        // 放大系数
        double amplificationCoefficient = 2.5D;

        // 分别计算字符串相似度和反转字符串后的相似度，然后求最大值
        double regularSimilarity = jaroDistance(a, b);
        double reverseSimilarity = jaroDistance(StringUtils.reverse(a), StringUtils.reverse(b));
        double similarity = Math.max(regularSimilarity, reverseSimilarity);

        similarity = similarity + (1 - similarity) * Math.exp(Math.negateExact((a.length() + b.length()) / 2) / amplificationCoefficient);
        similarity = Math.min(similarity, 1);

        // 修正单个字不同但相似度低于阈值情况
        if (similarity < threshold && a.length() == b.length() && a.length() > 3) {
            Collection<Integer> leftDiff = CollectionUtil.subtract(a.chars().boxed().collect(Collectors.toSet()), b.chars().boxed().collect(Collectors.toSet()));
            Collection<Integer> rightDiff = CollectionUtil.subtract(b.chars().boxed().collect(Collectors.toSet()), a.chars().boxed().collect(Collectors.toSet()));

            if (leftDiff.size() == 1 && rightDiff.size() == 1) {
                similarity = threshold + 0.01;
            }
        }
        return similarity;
    }

    /**
     * 字符串相似度比较算法jaro distance similarity的java版实现。
     *
     * @param a 字符串a
     * @param b 字符串b
     * @return 相似度
     * @see <a href="https://www.geeksforgeeks.org/jaro-and-jaro-winkler-similarity/?ref=lbp">jaro similarity</a>
     */
    public static double jaroDistance(String a, String b) {
        Assert.notBlank(a, "Argument 'a' cannot be null");
        Assert.notBlank(b, "Argument 'b' cannot be null");

        // If the Strings are equal
        if (a.equals(b)) {
            return 1.0;
        }

        // Maximum distance upto which matching
        // is allowed
        int maxDist = (int) (Math.floor(Math.max(a.length(), b.length()) / 2.00) - 1);

        // Hash for matches
        int[] aHash = new int[a.length()];
        int[] bHash = new int[b.length()];

        // Count of matches
        int match = 0;

        // Traverse through the first String
        for (int i = 0; i < a.length(); i++) {
            // Check if there is any matches
            for (int j = Math.max(0, i - maxDist); j < Math.min(b.length(), i + maxDist + 1); j++)
                // If there is a match
                if (a.charAt(i) == b.charAt(j) && bHash[j] == 0) {
                    aHash[i] = 1;
                    bHash[j] = 1;
                    match++;
                    break;
                }
        }

        // If there is no match
        if (match == 0)
            return 0.0;

        // Number of transpositions
        double t = 0;

        int point = 0;

        // Count number of occurrences
        // where two characters match but
        // there is a third matched character
        // in between the indices
        for (int i = 0; i < a.length(); i++)
            if (aHash[i] == 1) {
                // Find the next matched character
                // in second String
                while (bHash[point] == 0)
                    point++;

                if (a.charAt(i) != b.charAt(point++))
                    t++;
            }

        t /= 2;

        // Return the Jaro Similarity
        return (((double) match) / ((double) a.length())
                + ((double) match) / ((double) b.length())
                + ((double) match - t) / ((double) match))
                / 3.0;
    }

    /**
     * 正则匹配并提取文本
     *
     * @param regex  正则表达式
     * @param input  输入内容
     * @param number 文本匹配次数
     * @return 匹配到的文本
     */
    public static String regexMatch(String regex, String input, Integer number) {
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
}
