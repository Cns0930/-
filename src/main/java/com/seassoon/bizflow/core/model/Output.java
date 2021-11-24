package com.seassoon.bizflow.core.model;

import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.project.Project;
import com.seassoon.bizflow.core.model.rule.Approval;
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
    private House<DocumentKV> documentKvInfo;
    private House<Approval> ruleOutputData;
    private String ocrType;
    private Integer timeCost;

    @Data
    @EqualsAndHashCode
    public static class DocumentClassify {
        private List<Image> resultList = new ArrayList<>();

        public static DocumentClassify of(List<Image> images) {
            DocumentClassify classify = new DocumentClassify();
            classify.setResultList(images);
            return classify;
        }
    }

    @Data
    @EqualsAndHashCode
    public static class House<T> {
        private Room<T> resultList = new Room<>();

        public static <T> House<T> of(List<T> list) {
            House<T> house = new House<>();
            house.setResultList(Room.of(list));
            return house;
        }
    }

    @Data
    @EqualsAndHashCode
    public static class Room<T> {
        private List<T> kvList = new ArrayList<>();

        public static <T> Room<T> of(List<T> list) {
            Room<T> room = new Room<>();
            room.setKvList(list);
            return room;
        }
    }
}
