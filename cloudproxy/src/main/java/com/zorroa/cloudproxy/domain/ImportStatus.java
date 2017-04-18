package com.zorroa.cloudproxy.domain;

/**
 * Created by chambers on 3/28/17.
 */
public class ImportStatus {

    private long finishTime = 0;
    private long startTime = 0;
    private Long nextTime;
    private Boolean active;
    private Integer currentJobId;
    private Integer lastJobId;

    public long getFinishTime() {
        return finishTime;
    }

    public ImportStatus setFinishTime(long finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    public ImportStatus setStartTime(long startTime) {
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
}
