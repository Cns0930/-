package com.seassoon.bizflow.core.component.mq;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public class RedisSanctuary implements Sanctuary {

    private static final Logger logger = LoggerFactory.getLogger(RedisSanctuary.class);

    private Map<Integer, StringRedisTemplate> databaseRedisTemplate;
    private Duration timeout;
    private String inKey;
    private String outKey;
    private String timestampKey;

    @Override
    public Input get() {
        Input input = null;

        // Redis多库循环获取
        for (Integer database : databaseRedisTemplate.keySet()) {
            StringRedisTemplate redisTemplate = databaseRedisTemplate.get(database);
            String str = redisTemplate.opsForList().rightPop(inKey, timeout);
            // 捞到数据就返回
            if (StrUtil.isNotBlank(str)) {
                // 获取到数据的RedisDB绑定到当前线程上下文
                BizFlowContextHolder.setRedisDB(database);

                // Unicode转中文
                String jsonStr = UnicodeUtil.toString(str);
                input = JSONUtils.readValue(jsonStr, Input.class);
                if (input == null) {
                    logger.error("解析input.json出错");
                    continue;
                }

                // 获取当前线程上下文中保存的timestamp，放入Redis中
                long timestamp = BizFlowContextHolder.getTimestamp().toInstant(ZoneOffset.of("+8")).toEpochMilli();
                redisTemplate.opsForHash().put(timestampKey, input.getRecordId(), timestamp);
                break;
            }
        }

        return input;
    }

    @Override
    public void push(Output output) {
        Assert.notNull(output, "Argument 'output' cannot be null.");

        // 获取线程上下文中保存的RedisDB
        Integer database = BizFlowContextHolder.getRedisDB();
        if (database != null) {
            StringRedisTemplate redisTemplate = databaseRedisTemplate.get(database);

            // 获取当前RecordID的时间戳，分别从Redis和上下文获取，然后对比
            long timestamp = BizFlowContextHolder.getTimestamp().toInstant(ZoneOffset.of("+8")).toEpochMilli();
            Long rdsTimestamp = (Long) redisTemplate.opsForHash().get(timestampKey, output.getRecordId());
            if (rdsTimestamp != null && timestamp == rdsTimestamp) {
                // 删除旧纪录
                deleteIfExist(redisTemplate, outKey, output.getRecordId());

                // 最新的结果写回Redis
                String outputStr = JSONUtils.writeValueAsString(output);
                redisTemplate.opsForList().leftPush(outKey, outputStr);
            }
        }
    }

    private void deleteIfExist(StringRedisTemplate redisTemplate, String key, String recordId) {
        // 获取redis中的所有record，判断如果与当前传入的recordId相等，就删除
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size.intValue() > 0) {
            for (int i = 0; i < size; i++) {
                String str = redisTemplate.opsForList().index(key, i);
                if (str != null) {
                    Input input = JSONUtils.readValue(str, Input.class);
                    if (input != null && input.getRecordId().equals(recordId)) {
                        redisTemplate.opsForList().remove(key, 0, str);
                    }
                }
            }
        }
    }

    public void setDatabaseRedisTemplate(Map<Integer, StringRedisTemplate> databaseRedisTemplate) {
        this.databaseRedisTemplate = databaseRedisTemplate;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public void setInKey(String inKey) {
        this.inKey = inKey;
    }

    public void setOutKey(String outKey) {
        this.outKey = outKey;
    }

    public void setTimestampKey(String timestampKey) {
        this.timestampKey = timestampKey;
    }
}
