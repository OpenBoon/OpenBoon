package com.zorroa.cloudproxy.domain;

import java.util.List;

/**
 * Created by chambers on 3/24/17.
 */
public class Settings {

    private boolean startNow = false;
    private String archivistUrl;
    private String hmacKey;
    private String authUser;
    private List<String> paths;
    private String schedule;
    private int threads = 1;
    private Integer pipelineId;

    public String getArchivistUrl() {
        return archivistUrl;
    }

    public Settings setArchivistUrl(String archivistUrl) {
        this.archivistUrl = archivistUrl;
        return this;
    }

    public List<String> getPaths() {
        return paths;
    }

    public Settings setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public String getHmacKey() {
        return hmacKey;
    }

    public Settings setHmacKey(String hmacKey) {
        this.hmacKey = hmacKey;
        return this;
    }

    public String getSchedule() {
        return schedule;
    }

    public Settings setSchedule(String schedule) {
        this.schedule = schedule;
        return this;
    }

    public int getThreads() {
        return threads;
    }

    public Settings setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public boolean isStartNow() {
        return startNow;
    }

    public Settings setStartNow(boolean startNow) {
        this.startNow = startNow;
        return this;
    }

    public String getAuthUser() {
        return authUser;
    }

    public Settings setAuthUser(String authUser) {
        this.authUser = authUser;
        return this;
    }

    public Integer getPipelineId() {
        return pipelineId;
    }

    public Settings setPipelineId(Integer pipelineId) {
        this.pipelineId = pipelineId;
        return this;
    }
}
