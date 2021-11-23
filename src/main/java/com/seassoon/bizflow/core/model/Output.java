package com.seassoon.bizflow.core.model;

import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.extra.KeyValueInfo;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.project.Project;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class Output {
    private String version;
    private String sid;
    private String projectId;
    private String recordId;
    private Integer calcMode;
    private List<Image> imageList = new ArrayList<>();
    private ExtraInfo extraInfo;
    private Project projectInfo;
    private Integer approvalStage;
    private DocumentClassify documentClassify;
    private DocumentKVInfo documentKvInfo;

    @Data
    @EqualsAndHashCode
    public static class DocumentClassify {
        private List<Image> resultList = new ArrayList<>();
    }

    @Data
    @EqualsAndHashCode
    public static class DocumentKVInfo {

        private Result resultList;

        @Data
        @EqualsAndHashCode
        public static class Result {
            private List<KeyValueInfo> kvList = new ArrayList<>();
        }
    }
}
