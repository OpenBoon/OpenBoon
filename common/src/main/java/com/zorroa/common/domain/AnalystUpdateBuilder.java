package com.zorroa.common.domain;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystUpdateBuilder {

    private AnalystState state;
    private List<Float> load;
    private Map<String, Object> metrics;
    private int threadsUsed;
    private int queueSize;
    private long updatedTime;

    public long getUpdatedTime() {
        return updatedTime;
    }

    public AnalystUpdateBuilder setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public AnalystUpdateBuilder setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public AnalystUpdateBuilder setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }

    public int getThreadsUsed() {
        return threadsUsed;
    }

    public AnalystUpdateBuilder setThreadsUsed(int threadsUsed) {
        this.threadsUsed = threadsUsed;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public AnalystUpdateBuilder setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }


    public List<Float> getLoad() {
        return load;
    }

    public AnalystUpdateBuilder setLoad(List<Float> load) {
        this.load = load;
        return this;
    }
}
