package com.zorroa.common.domain;

import com.google.common.base.Objects;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 2/10/16.
 */
public class Analyst {

    private String id;
    private String url;
    private boolean data;
    private long startedTime;
    private AnalystState state;
    private int threadCount;
    private String arch;
    private String os;
    private List<Float> load;
    private Map<String, Object> metrics;
    private int threadsUsed;
    private int queueSize;
    private long updatedTime;
    private List<UUID> taskIds;

    public String getId() {
        return id;
    }

    public Analyst setId(String id) {
        this.id = id;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Analyst setUrl(String url) {
        this.url = url;
        return this;
    }

    public boolean isData() {
        return data;
    }

    public Analyst setData(boolean data) {
        this.data = data;
        return this;
    }

    public long getStartedTime() {
        return startedTime;
    }

    public Analyst setStartedTime(long startedTime) {
        this.startedTime = startedTime;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public Analyst setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public Analyst setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public String getArch() {
        return arch;
    }

    public Analyst setArch(String arch) {
        this.arch = arch;
        return this;
    }

    public String getOs() {
        return os;
    }

    public Analyst setOs(String os) {
        this.os = os;
        return this;
    }

    public List<Float> getLoad() {
        return load;
    }

    public Analyst setLoad(List<Float> load) {
        this.load = load;
        return this;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public Analyst setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }

    public int getThreadsUsed() {
        return threadsUsed;
    }

    public Analyst setThreadsUsed(int threadsUsed) {
        this.threadsUsed = threadsUsed;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public Analyst setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public Analyst setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public List<UUID> getTaskIds() {
        return taskIds;
    }

    public Analyst setTaskIds(List<UUID> taskIds) {
        this.taskIds = taskIds;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Analyst)) return false;
        Analyst analyst = (Analyst) o;
        return getId() == analyst.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
