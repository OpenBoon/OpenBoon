package com.zorroa.common.domain;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystUpdateSpec {

    private String url;
    private AnalystState state;
    private List<Float> load;
    private Map<String, Object> metrics;
    private int threadsUsed;
    private int queueSize;
    private int remainingCapacity;
    private long updatedTime;
    private List<Integer> taskIds;

    public String getUrl() {
        return url;
    }

    public AnalystUpdateSpec setUrl(String url) {
        this.url = url;
        return this;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public AnalystUpdateSpec setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public AnalystUpdateSpec setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public AnalystUpdateSpec setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }

    public int getThreadsUsed() {
        return threadsUsed;
    }

    public AnalystUpdateSpec setThreadsUsed(int threadsUsed) {
        this.threadsUsed = threadsUsed;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public AnalystUpdateSpec setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }


    public List<Float> getLoad() {
        return load;
    }

    public AnalystUpdateSpec setLoad(List<Float> load) {
        this.load = load;
        return this;
    }

    public List<Integer> getTaskIds() {
        return taskIds;
    }

    public AnalystUpdateSpec setTaskIds(List<Integer> taskIds) {
        this.taskIds = taskIds;
        return this;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public AnalystUpdateSpec setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
        return this;
    }
}
