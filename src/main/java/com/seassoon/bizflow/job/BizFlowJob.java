package com.seassoon.bizflow.job;

import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.LocalStorage;
import com.seassoon.bizflow.core.component.mq.Sanctuary;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.util.ConcurrentStopWatch;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.flow.Flow;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    private Sanctuary sanctuary;
    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private Flow flow;
    @Autowired
    private LocalStorage localStorage;

    @Scheduled(cron = "*/3 * * * * ?")
    public void execute() {
        // 初始化时间戳
        BizFlowContextHolder.setTimestamp();

        // 从Redis或者MQ获取待办事项
//        Input input = sanctuary.get();

        // 本地文件读取（本地测试用）
        Input input = readFile();
        if (input == null) {
            return;
        }

        // 计时器
        ConcurrentStopWatch stopWatch = new ConcurrentStopWatch();
        stopWatch.start(input.getRecordId());

        // 初始化BizFlow上下文
        BizFlowContextHolder.setInput(input);
        BizFlowContextHolder.putMDC(input.getRecordId());
        logger.info("从Redis获取事项成功：{} - {}", input.getRecordId(), input.getSid());

        // 交给流程处理器处理
        Output output = flow.process(input);

        // 结束计时，打印耗时表
        stopWatch.stop();
        logger.info("事项{}处理完成，耗时如下：\n{}", input.getRecordId(), stopWatch.prettyPrint());

        output.setOcrType(properties.getOcrType().toString());
        output.setTimeCost(Double.valueOf(stopWatch.getTotalTimeSeconds()).intValue());

        // 保存本地json文件
        localStorage.save(output, "output.json");

        // 结果返回给Redis
        sanctuary.push(output);

        // 清理BizFlow上下文
        BizFlowContextHolder.reset();
    }

    private Input readFile() {
        try {
            Path path = Paths.get("C:\\Users\\lw900\\Downloads\\1458638206939873282\\input.json");
            String str = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return JSONUtils.readValue(str, Input.class);
        } catch (IOException e) {
            logger.error("读取本地input.json出错" + e.getMessage());
            return null;
        }
    }
}
