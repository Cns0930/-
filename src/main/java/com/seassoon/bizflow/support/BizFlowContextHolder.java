package com.seassoon.bizflow.support;

import com.seassoon.bizflow.core.model.Input;
import org.springframework.core.NamedThreadLocal;

import java.time.LocalDateTime;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public class BizFlowContextHolder {

    private static final ThreadLocal<Input> RECORD_HOLDER = new NamedThreadLocal<>("BizFlow record");
    private static final ThreadLocal<LocalDateTime> TIMESTAMP_HOLDER = new NamedThreadLocal<>("Timestamp");
    private static final ThreadLocal<Integer> REDIS_DB_HOLDER = new NamedThreadLocal<>("Current redis database");

    public static void reset() {
        RECORD_HOLDER.remove();
        TIMESTAMP_HOLDER.remove();
        REDIS_DB_HOLDER.remove();
        removeMDC();
    }

    public static void setInput(Input input) {
        if (input == null) {
            reset();
        } else {
            RECORD_HOLDER.set(input);
        }
    }

    public static Input getInput() {
        return RECORD_HOLDER.get();
    }

    public static void setTimestamp() {
        TIMESTAMP_HOLDER.set(LocalDateTime.now());
    }

    public static LocalDateTime getTimestamp() {
        return TIMESTAMP_HOLDER.get();
    }

    /**
     * 将当前RecordID放入{@link org.slf4j.MDC}中
     *
     * @param recordId RecordID
     */
    public static void putMDC(String recordId) {
        org.slf4j.MDC.put("recordId", recordId);
    }

    public static void removeMDC() {
        org.slf4j.MDC.remove("recordId");
    }

    /**
     * 当前处理事项的RecordID所在RedisDB放入线程上下文
     *
     * @param redisDB RedisDB
     */
    public static void setRedisDB(Integer redisDB) {
        if (redisDB != null) {
            REDIS_DB_HOLDER.set(redisDB);
        }
    }
}
