package com.zorroa.archivist.domain;

import com.zorroa.common.domain.JobId;

/**
 * Created by chambers on 9/28/16.
 */
public class ExpiredJob implements JobId {

    private int id;
    private String logPath;

    public int getId() {
        return id;
    }

    public ExpiredJob setId(int id) {
        this.id = id;
        return this;
    }

    public String getLogPath() {
        return logPath;
    }

    public ExpiredJob setLogPath(String logPath) {
        this.logPath = logPath;
        return this;
    }

    @Override
    public Integer getJobId() {
        return id;
    }
}
