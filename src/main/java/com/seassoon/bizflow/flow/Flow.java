package com.seassoon.bizflow.flow;

import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.Output;

/**
 * 预审流程处理器
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Flow {

    /**
     * 处理预审流程
     *
     * @param input {@link Input}
     * @return {@link Output}
     */
    Output process(Input input);
}
