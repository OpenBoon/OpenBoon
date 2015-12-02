package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private AssetSearch search;
    private int size = 10;
    private int from;
    private int roomId;

    public AssetSearchBuilder() {
        search = new AssetSearch();
    }

    public AssetSearchBuilder(AssetSearch search) {
        this.search = search;
    }

    public AssetSearch getSearch() {
        return search;
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
