package com.seassoon.bizflow.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 集合相关工具类
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class Collections3 {

    /**
     * 合并多个list为一个
     *
     * @param lists 集合列表
     * @param <T>   元素类型
     * @return 新的集合
     */
    @SafeVarargs
    public static <T> List<T> merge(List<T>... lists) {
        List<T> list = new ArrayList<>();
        for (List<T> ts : lists) {
            list.addAll(ts);
        }
        return list;
    }
}
