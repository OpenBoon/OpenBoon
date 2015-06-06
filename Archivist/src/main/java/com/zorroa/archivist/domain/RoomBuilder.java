package com.zorroa.archivist.domain;

import java.util.Set;

public class RoomBuilder {

    private String name;
    private Set<String> inviteList;
    private String password;
    private boolean visible;

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
}
