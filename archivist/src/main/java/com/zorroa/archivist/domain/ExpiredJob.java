package com.zorroa.archivist.domain;

import java.util.UUID;

/**
 * Created by chambers on 9/28/16.
 */
public class ExpiredJob implements JobId {

    private UUID id;
    private String rootPath;

    public UUID getId() {
        return id;
    }

    public ExpiredJob setId(UUID id) {
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
    public UUID getJobId() {
        return id;
    }
}
