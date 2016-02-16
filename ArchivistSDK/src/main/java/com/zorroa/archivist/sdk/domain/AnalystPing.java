package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 2/10/16.
 */
public class AnalystPing {

    private String host;
    private int port;
    private boolean data = true;
    private int threadsTotal = -1;
    private int threadsActive = 0;
    private int processSuccess = 0;
    private int processFailed = 0;
    private int queueSize = 0;

    public AnalystPing() {}

    public AnalystPing(String host) {
        this.setHost(host);
    }

    public String getHost() {
        return host;
    }

    public AnalystPing setHost(String host) {
        this.host = host;
        return this;
    }

    public boolean isData() {
        return data;
    }

    public AnalystPing setData(boolean data) {
        this.data = data;
        return this;
    }

    public int getThreadsTotal() {
        return threadsTotal;
    }

    public AnalystPing setThreadsTotal(int threadsTotal) {
        this.threadsTotal = threadsTotal;
        return this;
    }

    public int getThreadsActive() {
        return threadsActive;
    }

    public AnalystPing setThreadsActive(int threadsActive) {
        this.threadsActive = threadsActive;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public AnalystPing setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public int getProcessFailed() {
        return processFailed;
    }

    public AnalystPing setProcessFailed(int processFailed) {
        this.processFailed = processFailed;
        return this;
    }

    public int getProcessSuccess() {
        return processSuccess;
    }

    public AnalystPing setProcessSuccess(int processSuccess) {
        this.processSuccess = processSuccess;
        return this;
    }

    public int getPort() {
        return port;
    }

    public AnalystPing setPort(int port) {
        this.port = port;
        return this;
    }
}
