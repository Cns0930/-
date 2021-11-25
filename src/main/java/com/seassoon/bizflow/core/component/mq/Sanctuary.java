package com.seassoon.bizflow.core.component.mq;

import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.Output;

/**
 * 接收MQ等中间件发来的数据（避难所？）
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Sanctuary {

    /**
     * 从指定Key中获取数据
     *
     * @return {@link Input}
     */
    Input get();

    /**
     * 将数据写回指定Key中
     *
     * @param output {@link Output}
     */
    void push(Output output);
}
