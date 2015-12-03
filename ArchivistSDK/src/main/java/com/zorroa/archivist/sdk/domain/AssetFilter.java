/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Restrict a query to a subset of assets matching the filter
 */
public class AssetFilter {
    private boolean selected;
    private List<String> assetIds;              // Filter to show the specified assets
    private List<Integer> exportIds;            // Filter to show the specified exports
    private List<String> folderIds;             // Filter to show the specified folders
    private List<String> existFields;           // Filter for assets that contain any of the specified fields
    private List<AssetFieldTerms> fieldTerms;   // Filter for matching terms in the specified field
    private List<AssetFieldRange> fieldRanges;  // Filter for terms within a specified range
    private List<AssetScript> scripts;          // Filter using the specified script and params

    public boolean isSelected() {
        return selected;
    }

    public AssetFilter setSelected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public List<String> getAssetIds() {
        return assetIds;
    }

    public AssetFilter setAssetIds(List<String> assetIds) {
        this.assetIds = assetIds;
        return this;
    }

    public List<Integer> getExportIds() {
        return exportIds;
    }

    public AssetFilter setExportIds(List<Integer> exportIds) {
        this.exportIds = exportIds;
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
