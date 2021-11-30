package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.extra.Field;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 文档元素检测器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Detector {

    /**
     * 检测文档元素
     *
     * @param params 参数列表，由调用者提供
     * @return {@link Field}
     */
    Field detect(Map<String, Object> params);


    // ---------- utils ----------

    /**
     * 判定目标A在B区域中的比例<p>
     * box : [y_start, x_start, y_end, x_end]
     *
     * @param a 目标区域
     * @param b 参考区域
     * @return 比例值
     */
    default Double getIOT(List<List<Integer>> a, List<List<Integer>> b) {
        Integer xA = Math.max(a.get(0).get(1), b.get(0).get(1));
        Integer yA = Math.max(a.get(0).get(0), b.get(0).get(0));
        Integer xB = Math.min(a.get(1).get(1), b.get(1).get(1));
        Integer yB = Math.min(a.get(1).get(0), b.get(1).get(0));

        int interArea = Math.max(0, xB - xA + 1) * Math.max(0, yB - yA + 1);
        int aArea = (a.get(1).get(1) - a.get(0).get(1) + 1) * (a.get(1).get(0) - a.get(0).get(0) + 1);
        return (double) interArea / aArea;
    }

    /**
     * 合并检测元素中的block属性
     *
     * @param elements 未合并的元素
     * @return 合并后坐标
     */
    @SuppressWarnings("unchecked")
    default List<List<Integer>> merge(List<Map<String, Object>> elements) {
        int xMin = 10000, yMin = 10000, xMax = 0, yMax = 0;
        if (elements.size() == 1) {
            return (List<List<Integer>>) elements.get(0).get("position");
        }

        for (Map<String, Object> element : elements) {
            List<List<Integer>> position = (List<List<Integer>>) element.get("position");
            xMin = Math.min(position.get(0).get(0), xMin);
            yMin = Math.min(position.get(0).get(1), yMin);
            xMax = Math.max(position.get(1).get(0), xMax);
            yMax = Math.max(position.get(1).get(1), yMax);
        }
        return Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax));
    }
}
