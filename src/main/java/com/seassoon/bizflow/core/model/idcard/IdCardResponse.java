package com.seassoon.bizflow.core.model.idcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@Data
@EqualsAndHashCode
public class IdCardResponse {
    private Double timeCost;
    private List<IdCard> blocks = new ArrayList<>();
    private String status;
    private String message;
}
