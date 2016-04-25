package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Created by chambers on 2/10/16.
 */
public class AnalystPing {

    private String url;
    private boolean data = true;
    private int threadsTotal = -1;
    private int threadsActive = 0;
    private int processSuccess = 0;
    private int processFailed = 0;
    private int queueSize = 0;
    private List<String> ingestProcessorClasses;

    public AnalystPing() {}

    public AnalystPing(String url) {
        this.setUrl(url);
    }

    public String getUrl() {
        return url;
    }

    public AnalystPing setUrl(String url) {
        this.url = url;
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

    public List<String> getIngestProcessorClasses() {
        return ingestProcessorClasses;
    }

    public AnalystPing setIngestProcessorClasses(List<String> ingestProcessorClasses) {
        this.ingestProcessorClasses = ingestProcessorClasses;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("url", url)
                .add("data", data)
                .add("threadsTotal", threadsTotal)
                .add("threadsActive", threadsActive)
                .add("processSuccess", processSuccess)
                .add("processFailed", processFailed)
                .add("queueSize", queueSize)
                .toString();
    }
}
