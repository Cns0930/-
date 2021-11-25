package com.seassoon.bizflow.core.model.extra;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class DocumentKV {
    private String documentLabel;
    private List<Content> documentList = new ArrayList<>();

    public static DocumentKV of(String documentLabel, List<Content> documentList) {
        DocumentKV docKV = new DocumentKV();
        docKV.setDocumentLabel(documentLabel);
        docKV.setDocumentList(documentList);
        return docKV;
    }
}
