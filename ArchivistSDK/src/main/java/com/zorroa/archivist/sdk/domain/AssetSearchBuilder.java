package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    /**
     * The keyword confidence level to search.  There are currently 5 confidence
     * buckets, with 5 being the most confident and 1 being the least.
     *
     * 0 = disabled (all keywords)
     * 5 = searches only confidence level 5
     * 4 = searched 4 and 5
     * 3 = searches 3,4 and 5
     *
     * You get the idea.
     *
     */
    private int confidence = 0;
    private AssetFilter filter;                 // Restrict results to match filter

    private AssetSearch search;
    private int size;
    private int from;
    private int room;

    public AssetSearchBuilder() { }

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

    public int getRoom() {
        return room;
    }

    public AssetSearchBuilder setRoom(int room) {
        this.room = room;
        return this;
    }

    public int getConfidence() {
        return confidence;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }

}
