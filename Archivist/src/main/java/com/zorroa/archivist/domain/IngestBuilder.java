package com.zorroa.archivist.domain;

import java.util.Set;

import com.zorroa.archivist.FileUtils;

public class IngestBuilder {

    private String path;
    private Set<String> fileTypes;
    private String proxyConfig = "standard";

    public IngestBuilder() { }

    public IngestBuilder(String path) {
        this.path = path;
        this.fileTypes = FileUtils.getSupportedImageFormats();
    }

    public IngestBuilder(String path, Set<String> fileTypes) {
        this.path = path;
        this.fileTypes = fileTypes;
    }

    public Set<String> getFileTypes() {
        if (fileTypes == null) {
            return FileUtils.getSupportedImageFormats();
        }
        return fileTypes;
    }

    public IngestBuilder setFileTypes(Set<String> fileTypes) {
        this.fileTypes = fileTypes;
        return this;
    }

    public String getPath() {
        return path;
    }

    public IngestBuilder setPath(String path) {
        this.path = path;
        return this;
    }

    public String getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(String proxyConfig) {
        this.proxyConfig = proxyConfig;
    }
}
