package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

import java.util.Set;

public class Room {

    private long id;
    private String session;
    private String name;
    private Set<String> inviteList;
    private boolean visible;

    public Room() { }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Room.class)
            .add("id", id)
            .add("session", session)
            .add("name", name)
            .add("visible", visible)
            .add("inviteList", inviteList)
            .toString();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getInviteList() {
        return inviteList;
    }

    public void setInviteList(Set<String> inviteList) {
        this.inviteList = inviteList;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
}
