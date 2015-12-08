package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 9/25/15.
 */
public class AssetSearchBuilder {

    private AssetSearch search;
    private int size = 10;
    private int from;

    /**
     * If this value is true, this search will be set as the current room search.
     */
    private boolean useAsRoomSearch = true;

    public AssetSearchBuilder() {
        search = new AssetSearch();
    }

    public AssetSearchBuilder(AssetSearch search) {
        this.search = search;
    }

    public AssetSearchBuilder(String query) {
        search = new AssetSearch();
        search.setQuery(query);
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

    public boolean isUseAsRoomSearch() {
        return useAsRoomSearch;
    }

    public void setUseAsRoomSearch(boolean useAsRoomSearch) {
        this.useAsRoomSearch = useAsRoomSearch;
    }

}
