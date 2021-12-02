package com.seassoon.bizflow.flow.extract.detect;

import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.extra.Field;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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

    /**
     * 是否勾选
     * @param params
     * @return
     */
    private Field detectCheckbox(Map<String, Object> params){
        Elements elements = (Elements) params.get("elements");
        if (elements != null) {
            if(!CollectionUtils.isEmpty(elements.getCheckbox())){
                // TODO 待我研究一下这个 location 是哪里来的
                return Field.of("已勾选", null, 1.0);
            }
        }
        return Field.of("未勾选", null, 1.0);
    }

}
