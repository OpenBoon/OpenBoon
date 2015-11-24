/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Restrict a query to a subset of assets matching the filter
 */
public class AssetFilter {
    private int exportId = 0;                   // Filter to show only this export
    private String createdBeforeTime;           // Eg. "now-1d" or see ES Range Query DSL for details:
    private String createdAfterTime;            //     https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html
    private List<String> folderIds;             // Filter to show the specified folders
    private List<String> existFields;           // Filter for assets that contain any of the specified fields
    private List<AssetFieldTerms> fieldTerms;   // Filter for matching terms in the specified field
    private List<AssetFieldRange> fieldRanges;  // Filter for terms within a specified range
    private List<AssetScript> scripts;          // Filter using the specified script and params

    public int getExportId() {
        return exportId;
    }

    public AssetFilter setExportId(int exportId) {
        this.exportId = exportId;
        return this;
    }

    public String getCreatedBeforeTime() {
        return createdBeforeTime;
    }

    public AssetFilter setCreatedBeforeTime(String createdBeforeTime) {
        this.createdBeforeTime = createdBeforeTime;
        return this;
    }

    public String getCreatedAfterTime() {
        return createdAfterTime;
    }

    public AssetFilter setCreatedAfterTime(String createdAfterTime) {
        this.createdAfterTime = createdAfterTime;
        return this;
    }

    public List<String> getFolderIds() {
        return folderIds;
    }

    public AssetFilter setFolderIds(List<String> folderIds) {
        this.folderIds = folderIds;
        return this;
    }

    public List<String> getExistFields() {
        return existFields;
    }

    public AssetFilter setExistFields(List<String> existFields) {
        this.existFields = existFields;
        return this;
    }

    public List<AssetFieldTerms> getFieldTerms() {
        return fieldTerms;
    }

    public AssetFilter setFieldTerms(List<AssetFieldTerms> fieldTerms) {
        this.fieldTerms = fieldTerms;
        return this;
    }

    public List<AssetFieldRange> getFieldRanges() {
        return fieldRanges;
    }

    public AssetFilter setFieldRanges(List<AssetFieldRange> fieldRanges) {
        this.fieldRanges = fieldRanges;
        return this;
    }

    public List<AssetScript> getScripts() {
        return scripts;
    }

    public AssetFilter setScripts(List<AssetScript> scripts) {
        this.scripts = scripts;
        return this;
    }
}
