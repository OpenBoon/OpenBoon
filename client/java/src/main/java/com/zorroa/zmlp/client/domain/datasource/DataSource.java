package com.zorroa.zmlp.client.domain.datasource;

import java.util.List;
import java.util.UUID;

/**
 * A DataSource describes a URI where Assets can be imported from.
 */
public class DataSource {

    /**
     * The Unique ID of the DataSource
     */
    private UUID id;
    /**
     * The unique name of the DataSource
     */
    private String name;
    /**
     * The URI of the DataSource
     */
    private String uri;
    /**
     * An optional credentials blob for the DataSource, this will be encrypted.
     */
    private String credentials;
    /**
     * A list of file type filters.
     */
    private List<String> fileTypes;
    /**
     * The default Analysis modules for this data source
     */
    private List<String> analysis;

    public DataSource() {
    }

    public UUID getId() {
        return id;
    }

    public DataSource setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DataSource setName(String name) {
        this.name = name;
        return this;
    }

    public String getUri() {
        return uri;
    }

    public DataSource setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public String getCredentials() {
        return credentials;
    }

    public DataSource setCredentials(String credentials) {
        this.credentials = credentials;
        return this;
    }

    public List<String> getFileTypes() {
        return fileTypes;
    }

    public DataSource setFileTypes(List<String> fileTypes) {
        this.fileTypes = fileTypes;
        return this;
    }

    public List<String> getAnalysis() {
        return analysis;
    }

    public DataSource setAnalysis(List<String> analysis) {
        this.analysis = analysis;
        return this;
    }
}
