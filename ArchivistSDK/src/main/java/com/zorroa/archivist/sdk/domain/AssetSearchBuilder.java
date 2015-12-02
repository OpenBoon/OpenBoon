package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private AssetSearch search;
    private int size = 10;
    private int from;
    private int roomId;

    public AssetSearchBuilder() { }

    public AssetSearchBuilder(AssetSearch search) {
        this.search = search;
    }

    public AssetSearch getSearch() {
        return search;
    }

    /**
     * Return true if a query string is set.
     *
     * @return
     */
    public boolean isQuerySet() {
        return (search != null && search.getQuery() != null && search.getQuery().length() > 0);
    }

    public AssetSearchBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public int getSize() {
        return size;
    }

    public AssetSearchBuilder setSize(int size) {
        this.size = size;
        return this;
    }

    public int getFrom() {
        return from;
    }

    public AssetSearchBuilder setFrom(int from) {
        this.from = from;
        return this;
    }

    public int getRoomId() {
        return roomId;
    }

    public AssetSearchBuilder setRoom(int room) {
        this.roomId = roomId;
        return this;
    }
}
