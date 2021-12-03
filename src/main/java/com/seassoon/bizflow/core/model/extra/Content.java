package com.seassoon.bizflow.core.model.extra;

import com.google.common.collect.ImmutableMap;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
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

    public static Content of(String imageId, CheckpointConfig.ExtractPoint extractPoint) {
        Content content = new Content();
        content.setImageId(imageId);
        content.setDocumentField(extractPoint.getDocumentField());
        content.setPage(extractPoint.getPage());
        content.setImageOrString(Content.VALUE_TYPES.get(extractPoint.getValueType()));
        content.setSource("smj"); // 默认为扫描件
        content.setSortProperty(extractPoint.getSortProperty());
        content.setDisplayProperty(extractPoint.getDisplayProperty());
        return content;
    }
}
