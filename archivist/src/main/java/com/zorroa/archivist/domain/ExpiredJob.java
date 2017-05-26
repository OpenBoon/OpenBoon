package com.zorroa.archivist.domain;

/**
 * Created by chambers on 9/28/16.
 */
public class ExpiredJob implements JobId {

    private int id;
    private String rootPath;

    public int getId() {
        return id;
    }

    public ExpiredJob setId(int id) {
        this.id = id;
        return this;
    }

    public String getLogPath() {
        return rootPath.concat("/logs");
    }
    public String getExportedPath() {
        return rootPath.concat("/exported");
    }

    public String getRootPath() {
        return rootPath;
    }

    public ExpiredJob setRootPath(String logPath) {
        this.rootPath = logPath;
        return this;
    }

    @Override
    public Integer getJobId() {
        return id;
    }
}
