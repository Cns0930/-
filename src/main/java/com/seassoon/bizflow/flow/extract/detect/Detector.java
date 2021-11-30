package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.extra.Field;

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
}
