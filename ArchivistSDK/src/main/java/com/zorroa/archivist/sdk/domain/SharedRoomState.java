package com.zorroa.archivist.sdk.domain;

import java.util.Set;

/**
 * The current shared state of a room.
 */
public class SharedRoomState {

    /**
     * The last search of a room.
     */
    private AssetSearchBuilder search;

    /**
     * The last selection made in a room.
     */
    private Set<String> selection;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    private int version;

    public AssetSearchBuilder getSearch() {
        return search;
    }

    public void setSearch(AssetSearchBuilder search) {
        this.search = search;
    }

    public Set<String> getSelection() {
        return selection;
    }

    public void setSelection(Set<String> selection) {
        this.selection = selection;
    }
}
