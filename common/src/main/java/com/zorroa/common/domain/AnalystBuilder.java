package com.zorroa.common.domain;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystBuilder extends AnalystUpdateBuilder {

    private String url;
    private boolean data;
    private long startedTime;
    private AnalystState state;
    private int threadCount;
    private String arch;
    private String os;

    public String getUrl() {
        return url;
    }

    public AnalystBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    public boolean isData() {
        return data;
    }

    public AnalystBuilder setData(boolean data) {
        this.data = data;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public AnalystBuilder setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public AnalystBuilder setThreadCount(int threadCount) {
        this.threadCount = threadCount;
        return this;
    }

    public String getArch() {
        return arch;
    }

    public AnalystBuilder setArch(String arch) {
        this.arch = arch;
        return this;
    }

    public String getOs() {
        return os;
    }

    public AnalystBuilder setOs(String os) {
        this.os = os;
        return this;
    }

    public long getStartedTime() {
        return startedTime;
    }

    public AnalystBuilder setStartedTime(long startedTime) {
        this.startedTime = startedTime;
        return this;
    }
}
