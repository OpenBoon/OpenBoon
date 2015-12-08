package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Sets;

import java.util.Set;

public class RoomBuilder {

    private Long sessionId = null;
    private String name;
    private Set<String> inviteList;
    private String password;
    private boolean visible = true;

    /**
     * The current search at room creation time, if any.
     */
    private AssetSearchBuilder search;

    /**
     * The  selected assets at room creation time, if any.
     */
    private Set<String> selection;

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
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isVisible() {
        return visible;
    }
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    public Long getSessionId() {
        return sessionId;
    }
    public void setSessionId(Long session) {
        this.sessionId = session;
    }

    public AssetSearchBuilder getSearch() {
        if (search == null) {
            search = new AssetSearchBuilder();
        }
        return search;
    }

    public void setSearch(AssetSearchBuilder search) {
        this.search = search;
    }

    public Set<String> getSelection() {
        if (selection == null) {
            selection = Sets.newHashSet();
        }
        return selection;
    }

    public void setSelection(Set<String> selection) {
        this.selection = selection;
    }
}
