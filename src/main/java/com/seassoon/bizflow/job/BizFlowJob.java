package com.seassoon.bizflow.job;

import cn.hutool.core.text.UnicodeUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.flow.Flow;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

/**
 * 预审服务Job。
 * </b>
 *
 * 该Job监听Redis队列中的input.json，后续交给预审过程处理
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class BizFlowJob {

    private static final Logger logger = LoggerFactory.getLogger(BizFlowJob.class);

    @Autowired
    private Map<Integer, StringRedisTemplate> databaseRedisTemplate;
    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private Flow flow;

    @Scheduled(cron = "*/3 * * * * ?")
    public void execute() {
        // 读取配置文件中的key
        String key = properties.getRedis().getQueue().get(BizFlowProperties.Redis.Queue.TODO);
        Duration timeout = properties.getRedis().getTimeout();

//        String jsonStr = redisTemplate.opsForList().rightPop(key, timeout);
        String jsonStr = "";
        try {
            jsonStr = new String(Files.readAllBytes(Paths.get("C:\\Users\\lw900\\Downloads\\1458638206939873282\\input.json")), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 计时器
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        if (StringUtils.isNotBlank(jsonStr)) {
            // unicode转中文
            jsonStr = UnicodeUtil.toString(jsonStr);
            Input input = JSONUtils.readValue(jsonStr, Input.class);
            if (input == null) {
                return;
            }

            // 开始计时
            stopWatch.start(input.getRecordId());

            // 初始化BizFlow上下文
            BizFlowContextHolder.setInput(input);
            BizFlowContextHolder.setTimestamp();
            BizFlowContextHolder.putMDC(input.getRecordId());
            logger.info("从Redis获取事项成功：{} - {}", input.getRecordId(), input.getSid());

            // 交给流程处理器处理
            Output output = flow.process(input);

            // TODO 结果返回给Redis

            // 结束计时，打印耗时表
            stopWatch.stop();
            logger.info("{}处理完成，耗时如下：\n{}", input.getRecordId(), stopWatch.prettyPrint());

            // 清理BizFlow上下文
            BizFlowContextHolder.reset();
        }
    }
}
