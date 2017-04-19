package com.zorroa.cloudproxy.domain;

import java.util.Map;

/**
 * Created by chambers on 3/28/17.
 */
public class ImportStatus {

    private Long finishTime;
    private Long startTime;
    private Long nextTime;
    private Boolean active;
    private Integer currentJobId;

    private Map<String, Object> progress;

    public Long getFinishTime() {
        return finishTime;
    }

    public ImportStatus setFinishTime(Long finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public Long getStartTime() {
        return startTime;
    }

    public ImportStatus setStartTime(Long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Long getNextTime() {
        return nextTime;
    }

    public ImportStatus setNextTime(Long nextTime) {
        this.nextTime = nextTime;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public ImportStatus setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public Integer getCurrentJobId() {
        return currentJobId;
    }

    public ImportStatus setCurrentJobId(Integer currentJobId) {
        this.currentJobId = currentJobId;
        return this;
    }

    public Map<String, Object> getProgress() {
        return progress;
    }

    public ImportStatus setProgress(Map<String, Object> progress) {
        this.progress = progress;
        return this;
    }
}
