package com.zorroa.zmlp.sdk.domain;

import java.util.List;

public class DataSourceSpec {

    private String name;
    private String uri;
    private String credentials;
    private List<String> fileTypes;
    private List<String> analysis;

    public String getName() {
        return name;
    }

    public DataSourceSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public DataSourceSpec setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getCredentials() {
        return credentials;
    }

    public DataSourceSpec setCredentials(String credentials) {
        this.credentials = credentials;
        return this;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

    public DataSourceSpec setFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
        return this;
    }

    public List<String> getAnalysis() {
        return analysis;
    }

    public DataSourceSpec setAnalysis(List<String> analysis) {
        this.analysis = analysis;
        return this;
    }
}
