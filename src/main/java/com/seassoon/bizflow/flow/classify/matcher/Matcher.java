package com.seassoon.bizflow.flow.classify.matcher;

import com.seassoon.bizflow.core.model.config.SortConfig;

import java.util.List;

/**
 * 分类文档匹配器接口。
 * <p>
 * 对分类配置中typeIdField字段的规则做进一步抽象，后续可根据业务变化扩展该接口，实现更多规则。
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Matcher {

    /**
     * 文本行匹配文档分类
     *
     * @param typeIdField 文档分类配置
     * @param texts    文本行（OCR识别结果）
     * @return 匹配成功返回true
     */
    boolean match(SortConfig.TypeIdField typeIdField, List<String> texts);
}
