package com.seassoon.bizflow.core.model.extra;

import com.google.common.collect.ImmutableMap;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Content {
    private String imageId;
    private String documentField;
    private Integer page;
    // 这个属性好像没什么卵用（是否可以去掉）
    private String isTrue;
    private String imageOrString;
    private List<Field> valueInfo = new ArrayList<>();
    private String source;
    private String sortProperty;
    private String displayProperty;

    /** 对应ExtractPoint中的value_info与image_or_string的值映射 */
    public static Map<String, String> VALUE_TYPES = ImmutableMap.<String, String> builder()
            .put("string", "0")
            .put("img", "1")
            .build();
}
