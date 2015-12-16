package com.zorroa.archivist.sdk.domain;

import java.util.Set;

/**
 * Created by chambers on 9/26/15.
 */
public class RoomUpdateBuilder {

    private String name;
    private Set<String> inviteList;
    private String password;

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

}
