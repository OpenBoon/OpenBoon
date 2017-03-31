package com.zorroa.cloudproxy.domain;

/**
 * Created by chambers on 3/28/17.
 */
public class ImportStats {

    private long finishTime = 0;
    private long startTime = 0;
    private Long nextTime;

    public long getFinishTime() {
        return finishTime;
    }

    public ImportStats setFinishTime(long finishTime) {
        this.finishTime = finishTime;
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    public ImportStats setStartTime(long startTime) {
        this.startTime = startTime;
        return this;
    }

    public Long getNextTime() {
        return nextTime;
    }

    public ImportStats setNextTime(Long nextTime) {
        this.nextTime = nextTime;
        return this;
    }
}
