package com.seassoon.bizflow.core.component.mq;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public class RedisSanctuary implements Sanctuary {

    private Map<Integer, StringRedisTemplate> databaseRedisTemplate;
    private Duration timeout;

    @Override
    public String get(String channel) {
        String str = null;

        // Redis多库循环获取
        for (Integer database : databaseRedisTemplate.keySet()) {
            StringRedisTemplate redisTemplate = databaseRedisTemplate.get(database);
            str = redisTemplate.opsForList().rightPop(channel, timeout);
            // 捞到数据就返回
            if (StrUtil.isNotBlank(str)) {
                // 获取到数据的RedisDB绑定到当前线程上下文
                BizFlowContextHolder.setRedisDB(database);
                break;
            }
        }

        return str;
    }

    @Override
    public void push(String channel, String str) {
        Assert.notBlank(str, "Argument 'str' cannot be null or blank string.");

        // 获取线程上下文中保存的RedisDB
        Integer database = BizFlowContextHolder.getRedisDB();
        if (database != null) {
            // 结果写回Redis
            StringRedisTemplate redisTemplate = databaseRedisTemplate.get(database);
            redisTemplate.opsForList().leftPush(channel, str);
        }
    }

    public void setDatabaseRedisTemplate(Map<Integer, StringRedisTemplate> databaseRedisTemplate) {
        this.databaseRedisTemplate = databaseRedisTemplate;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
}
