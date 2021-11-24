package com.seassoon.bizflow.core.component.mq;

/**
 * 接收MQ等中间件发来的数据（避难所？）
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public interface Sanctuary {

    /**
     * 从指定Key中获取数据
     *
     * @param channel Key
     * @return JSON字符串
     */
    String get(String channel);

    /**
     * 将数据写回指定Key中
     *
     * @param channel Key
     * @param str json字符串
     */
    void push(String channel, String str);
}
