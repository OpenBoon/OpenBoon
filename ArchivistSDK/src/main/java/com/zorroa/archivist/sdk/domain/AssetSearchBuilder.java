package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private String query;
    private int room = 0;
    private int exportId = 0;

    /*
     * Uses some standard time query format. (now-1d for example)
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html
     */
    public String createdBeforeTime;
    public String createdAfterTime;
    public List<String> folderIds;

    public int getRoom() {
        return room;
    }

    public AssetSearchBuilder setRoom(int room) {
        this.room = room;
        return this;
    }

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
}
