package com.zorroa.archivist.sdk.domain;

import com.google.common.base.Objects;

import java.util.List;

/**
 * Created by chambers on 2/10/16.
 */
public class Analyst {

    private int id;
    private String url;
    private AnalystState state;
    private boolean data;
    private int threadsTotal;
    private int threadsActive;
    private int processSuccess;
    private int processFailed;
    private int queueSize;
    private List<String> ingestProcessorClasses;

    public String getUrl() {
        return url;
    }

    public Analyst setUrl(String url) {
        this.url = url;
        return this;
    }

    public AnalystState getState() {
        return state;
    }

    public Analyst setState(AnalystState state) {
        this.state = state;
        return this;
    }

    public boolean isData() {
        return data;
    }

    public Analyst setData(boolean data) {
        this.data = data;
        return this;
    }

    public int getThreadsTotal() {
        return threadsTotal;
    }

    public Analyst setThreadsTotal(int threadsTotal) {
        this.threadsTotal = threadsTotal;
        return this;
    }

    public int getThreadsActive() {
        return threadsActive;
    }

    public Analyst setThreadsActive(int threadsActive) {
        this.threadsActive = threadsActive;
        return this;
    }

    public int getProcessSuccess() {
        return processSuccess;
    }

    public Analyst setProcessSuccess(int processSuccess) {
        this.processSuccess = processSuccess;
        return this;
    }

    public int getProcessFailed() {
        return processFailed;
    }

    public Analyst setProcessFailed(int processFailed) {
        this.processFailed = processFailed;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public Analyst setQueueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }

    public int getId() {
        return id;
    }

    public Analyst setId(int id) {
        this.id = id;
        return this;
    }

    public List<String> getIngestProcessorClasses() {
        return ingestProcessorClasses;
    }

    public Analyst setIngestProcessorClasses(List<String> ingestProcessorClasses) {
        this.ingestProcessorClasses = ingestProcessorClasses;
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
