package com.seassoon.bizflow.core.util;

import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static boolean checkValue(Object object) {
        if (object instanceof String && "".equals(object)) {
            return false;
        }
        if (ObjectUtils.isEmpty(object)) {
            return false;
        }
        return true;
    }

    public static Map<String, Object> parseMapForFilter(Map<String, Object> map) {
        if (map == null) {
            return null;
        } else {
            map = map.entrySet().stream()
                    .filter((e) -> checkValue(e.getValue()))
                    .collect(Collectors.toMap(
                            (e) -> (String) e.getKey(),
                            (e) -> e.getValue()
                    ));
        }
        return map;
    }
}
