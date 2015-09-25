package com.zorroa.archivist.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private String query;
    private int room = 0;

    /*
     * Uses some standard time query format. (now-1d for example)
     * https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-range-query.html
     */
    public String createdBeforeTime;
    public String createdAfterTime;

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCreatedBeforeTime() {
        return createdBeforeTime;
    }

    public void setCreatedBeforeTime(String createdBeforeTime) {
        this.createdBeforeTime = createdBeforeTime;
    }

    public String getCreatedAfterTime() {
        return createdAfterTime;
    }

    public void setCreatedAfterTime(String createdAfterTime) {
        this.createdAfterTime = createdAfterTime;
    }
}
