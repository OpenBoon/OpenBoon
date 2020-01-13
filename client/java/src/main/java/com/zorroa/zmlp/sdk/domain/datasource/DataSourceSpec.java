package com.zorroa.zmlp.sdk.domain.datasource;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a DataSource containing assets to import.
 */
public class DataSourceSpec {

    /**
     * The name of the DataSource
     */
    private String name;
    /**
     * The URI the DataSource points to.
     */
    private String uri;
    /**
     * An optional credentials blob for the DataSource, this will be encrypted.
     */
    private String credentials;
    /**
     * A list of file extensions to filter
     */
    private List<String> fileTypes;
    /**
     * A list of analysis modules to apply to the DataSource,
     * this overrides project defaults if set.
     */
    private  List<String> analysis;

    public DataSourceSpec() {
        fileTypes = new ArrayList();
        analysis = new ArrayList();
    }

    public DataSourceSpec withName(String name) {
        this.name = name;
        return this;
    }

    public DataSourceSpec withUri(String uri) {
        this.uri = uri;
        return this;
    }

    public DataSourceSpec withCredentials(String credentials) {
        this.credentials = credentials;
        return this;
    }

    public DataSourceSpec withFileTypes(List<String> fileTypes){
        this.fileTypes = fileTypes;
        return this;
    }

    public DataSourceSpec withAnalysis(List<String> analysis){
        this.analysis = analysis;
        return this;
    }

    public DataSourceSpec addFileTypes(String fileType){
        this.fileTypes.add(fileType);
        return this;
    }

    public DataSourceSpec addAnalysis(String analysis){
        this.analysis.add(analysis);
        return this;
    }

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
