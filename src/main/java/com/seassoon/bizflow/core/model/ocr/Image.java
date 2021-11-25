package com.seassoon.bizflow.core.model.ocr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Image {
    private String imageId;
    private String imageUrl;
    private String documentName;
    private String documentLabel;
    private Integer documentSource;
    private String correctedImageUrl;
    private Integer documentPage;
    private Integer totalPages;
    private Integer processMode;
    @JsonProperty("isWithTitle")
    private String isWithTitle;

    // 已分类图片本地保存路径
    @JsonIgnore
    private String classifiedPath;

    // 图像角度
    @JsonIgnore
    private Double imageAngle;

    // 表格切分后的cell存放路径
    @JsonIgnore
    private List<String> tableCells;
}
