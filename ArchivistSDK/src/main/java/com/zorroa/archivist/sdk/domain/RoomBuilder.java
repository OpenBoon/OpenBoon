package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Sets;

import java.util.Set;

public class RoomBuilder {

    /**
     * The name of the room.
     */
    private String name;

    /**
     * A password for entering the room.
     */
    private String password;

    /**
     * Determines if the room is visible or not.
     */
    private boolean visible = true;

    /**
     * The current search at room creation time, if any.
     */
    private AssetSearch search;

    /**
     * The  selected assets at room creation time, if any.
     */
    private Set<String> selection;

    public RoomBuilder() { }

    public RoomBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public RoomBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public RoomBuilder setPassword(String password) {
        this.password = password;
        return this;
    }
    public boolean isVisible() {
        return visible;
    }

    public RoomBuilder setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public AssetSearch getSearch() {
        if (search == null) {
            search = new AssetSearch();
        }
        return search;
    }

    public RoomBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Set<String> getSelection() {
        if (selection == null) {
            selection = Sets.newHashSet();
        }
        return selection;
    }

    public RoomBuilder setSelection(Set<String> selection) {
        this.selection = selection;
        return this;
    }
}
