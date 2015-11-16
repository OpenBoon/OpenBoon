package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 11/1/15.
 */
public class Export {

    private int id;
    private String note;
    private long timeCreated;
    private String userCreated;
    private ExportState state;

    private AssetSearchBuilder search;
    private ExportOptions options;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(String userCreated) {
        this.userCreated = userCreated;
    }

    public ExportState getState() {
        return state;
    }

    public void setState(ExportState state) {
        this.state = state;
    }

    public AssetSearchBuilder getSearch() {
        return search;
    }

    public void setSearch(AssetSearchBuilder search) {
        this.search = search;
    }

    public ExportOptions getOptions() {
        return options;
    }

    public void setOptions(ExportOptions options) {
        this.options = options;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Export export = (Export) o;
        return id == export.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
