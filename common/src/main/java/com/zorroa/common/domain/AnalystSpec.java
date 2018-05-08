package com.zorroa.common.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystSpec {

    private String id;
    private boolean data;
    private AnalystState state;
    private int threadCount;
    private String arch;
    private String os;
    private List<UUID> taskIds;
    private String url;
    private String version;

    private Map<String, Object> metrics;
    private int threadsUsed;
    private int queueSize;
    private long updatedTime;

    @Deprecated
    private List<Float> load;
    private double loadAvg;

    public String getId() {
        return id;
    }

    public AnalystSpec setId(String id) {
        this.id = id;
        return this;
    }

    public boolean isData() {
        return data;
    }

    public AnalystSpec setData(boolean data) {
        this.data = data;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public AnalystSpec setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public AnalystSpec setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public String getArch() {
        return arch;
    }

    public AnalystSpec setArch(String arch) {
        this.arch = arch;
        return this;
    }

    public String getOs() {
        return os;
    }

    public AnalystSpec setOs(String os) {
        this.os = os;
        return this;
    }

    public List<UUID> getTaskIds() {
        return taskIds;
    }

    public AnalystSpec setTaskIds(List<UUID> taskIds) {
        this.taskIds = taskIds;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public AnalystSpec setUrl(String url) {
        this.url = url;
        return this;
    }

    public List<Float> getLoad() {
        return load;
    }

    public AnalystSpec setLoad(List<Float> load) {
        this.load = load;
        return this;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public AnalystSpec setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }

    public int getThreadsUsed() {
        return threadsUsed;
    }

    public AnalystSpec setThreadsUsed(int threadsUsed) {
        this.threadsUsed = threadsUsed;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public AnalystSpec setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public AnalystSpec setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
        return this;
    }

    public double getLoadAvg() {
        return loadAvg;
    }

    public AnalystSpec setLoadAvg(double loadAvg) {
        this.loadAvg = loadAvg;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public AnalystSpec setVersion(String version) {
        this.version = version;
        return this;
    }
}
