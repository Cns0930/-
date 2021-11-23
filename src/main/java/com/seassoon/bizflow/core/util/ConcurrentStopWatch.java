package com.seassoon.bizflow.core.util;

import org.springframework.util.StopWatch;

import java.text.NumberFormat;

/**
 * 对{@link org.springframework.util.StopWatch}的封装，使其支持多线程环境
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class ConcurrentStopWatch {

    private final ThreadLocal<StopWatch> stopWatch;

    public ConcurrentStopWatch() {
        this("");
    }

    public ConcurrentStopWatch(String id) {
        this.stopWatch = ThreadLocal.withInitial(() -> new StopWatch(id));
    }

    public void start() {
        start("");
    }

    public void start(String taskName) {
        if (!this.stopWatch.get().isRunning()) {
            this.stopWatch.get().start(taskName);
        }
    }

    public void stop() {
        if (this.stopWatch.get().isRunning()) {
            this.stopWatch.get().stop();
        }
    }

    public double getTotalTimeSeconds() {
        return this.stopWatch.get().getTotalTimeSeconds();
    }

    public String prettyPrint() {
        StopWatch stopWatch = this.stopWatch.get();

        StringBuilder sb = new StringBuilder("StopWatch '" + stopWatch.getId() + "': running time = " + stopWatch.getTotalTimeSeconds() + " s");
        sb.append('\n');

        sb.append("---------------------------------------------\n");
        sb.append("Second    %     Task name\n");
        sb.append("---------------------------------------------\n");
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumIntegerDigits(1);
        nf.setGroupingUsed(false);
        NumberFormat pf = NumberFormat.getPercentInstance();
        pf.setMinimumIntegerDigits(3);
        pf.setGroupingUsed(false);
        for (StopWatch.TaskInfo task : stopWatch.getTaskInfo()) {
            sb.append(nf.format(task.getTimeSeconds())).append("  ");
            sb.append(pf.format((double) task.getTimeSeconds() / getTotalTimeSeconds())).append("  ");
            sb.append(task.getTaskName()).append('\n');
        }
        return sb.toString();
    }
}
