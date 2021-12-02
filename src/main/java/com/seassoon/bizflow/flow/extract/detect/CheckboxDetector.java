package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.extra.Field;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 是否勾选/勾选内容
 * @author chimney
 * @date 2021/12/1
 */
@Component
public class CheckboxDetector extends DocElementDetector{


    @Override
    public Field detectField(Map<String, Object> params) {
        return null;
    }
}
