package com.zorroa.common.domain;

/**
 * Created by chambers on 6/16/16.
 */
public class AnalystSpec extends AnalystUpdateSpec {

    private boolean data;
    private long startedTime;
    private AnalystState state;
    private int threadCount;
    private String arch;
    private String os;
    private String id;

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

    public long getStartedTime() {
        return startedTime;
    }

    public AnalystSpec setStartedTime(long startedTime) {
        this.startedTime = startedTime;
        return this;
    }
}
