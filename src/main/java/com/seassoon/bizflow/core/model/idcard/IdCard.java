package com.seassoon.bizflow.core.model.idcard;

import com.seassoon.bizflow.core.model.ocr.Position;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class IdCard {
    private Double objScore;
    private String frontOrBack;
    private List<Position> position = new ArrayList<>();
    private List<IdInfo> info = new ArrayList<>();
    private String url;
}
