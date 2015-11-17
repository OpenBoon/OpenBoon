package com.zorroa.archivist.sdk.domain;

import java.util.List;

/**
 * Used for returning a filtered list of exports.
 */
public class ExportFilter {

    private List<String> users;
    private List<ExportState> states;
    private long beforeTime = -1;
    private long afterTime = -1;
    private int page = 1;


    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public long getBeforeTime() {
        return beforeTime;
    }

    public void setBeforeTime(long beforeTime) {
        this.beforeTime = beforeTime;
    }

    public long getAfterTime() {
        return afterTime;
    }

    public void setAfterTime(long afterTime) {
        this.afterTime = afterTime;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<ExportState> getStates() {
        return states;
    }

    public void setStates(List<ExportState> states) {
        this.states = states;
    }
}
