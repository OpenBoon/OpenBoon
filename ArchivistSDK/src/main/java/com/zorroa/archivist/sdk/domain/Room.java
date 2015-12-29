package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

public class Room implements Id {

    private int id;
    private String name;
    private boolean visible;

    public Room() { }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Room.class)
            .add("id", id)
            .add("name", name)
            .add("visible", visible)
            .toString();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
