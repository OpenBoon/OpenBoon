package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.elasticsearch.common.base.MoreObjects;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Room {

    private long id;
    private String session;
    private String name;
    private Set<String> inviteList;
    private boolean visible;
    private String folderId;

    public Room() { }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Room.class)
            .add("id", id)
            .add("session", session)
            .add("name", name)
            .add("visible", visible)
            .add("inviteList", inviteList)
            .add("folderId", folderId)
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

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
}
