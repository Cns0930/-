package com.seassoon.bizflow.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
@EnableScheduling
@SpringBootConfiguration
public class ScheduledConfiguration implements SchedulingConfigurer {

    @Autowired
    private BizFlowProperties properties;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 配置JOB使用的线程池
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    @Bean
    public TaskScheduler taskScheduler() {
        BizFlowProperties.Schedule schedule = properties.getSchedule();

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(schedule.getPoolSize());
        taskScheduler.setThreadNamePrefix(schedule.getThreadNamePrefix());
        taskScheduler.setWaitForTasksToCompleteOnShutdown(schedule.getWaitForTaskCompletedOnShutdown());
        taskScheduler.setAwaitTerminationSeconds(schedule.getPoolSize());
        return taskScheduler;
    }
}
