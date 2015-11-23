package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private String query;                       // Eg. "food and dog", or see ES Query String DSL for details
    private int exportId = 0;                   // Filter to show only this export
    private String createdBeforeTime;           // Eg. "now-1d" or see ES Range Query DSL for details:
    private String createdAfterTime;            //     https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html
    private List<String> folderIds;             // Filter to show the specified folders
    private List<String> existFields;           // Filter for assets that contain any of the specified fields
    private List<AssetFieldTerms> fieldTerms;   // Filter for matching terms in the specified field
    private List<AssetFieldRange> fieldRanges;  // Filter for terms within a specified range
    private List<AssetScript> scripts;          // Filter using the specified script and params

    public AssetSearchBuilder() { }

    public String getQuery() {
        return query;
    }

    public AssetSearchBuilder setQuery(String query) {
        this.query = query;
        return this;
    }

    public String getCreatedBeforeTime() {
        return createdBeforeTime;
    }

    public AssetSearchBuilder setCreatedBeforeTime(String createdBeforeTime) {
        this.createdBeforeTime = createdBeforeTime;
        return this;
    }

    public String getCreatedAfterTime() {
        return createdAfterTime;
    }

    public AssetSearchBuilder setCreatedAfterTime(String createdAfterTime) {
        this.createdAfterTime = createdAfterTime;
        return this;
    }

    public List<String> getFolderIds() {
        return folderIds;
    }

    public AssetSearchBuilder setFolderIds(List<String> folderIds) {
        this.folderIds = folderIds;
        return this;
    }

    public AssetSearchBuilder setExportId(int exportId) {
        this.exportId = exportId;
        return this;
    }

    public int getExportId() {
        return this.exportId;
    }

    public List<String> getExistFields() {
        return existFields;
    }

    public void setExistFields(List<String> existFields) {
        this.existFields = existFields;
    }

    public List<AssetFieldTerms> getFieldTerms() {
        return fieldTerms;
    }

    public void setFieldTerms(List<AssetFieldTerms> fieldTerms) {
        this.fieldTerms = fieldTerms;
    }

    public List<AssetFieldRange> getFieldRanges() {
        return fieldRanges;
    }

    public void setFieldRanges(List<AssetFieldRange> fieldRanges) {
        this.fieldRanges = fieldRanges;
    }

    public List<AssetScript> getScripts() {
        return scripts;
    }

    public void setScripts(List<AssetScript> scripts) {
        this.scripts = scripts;
    }
}
