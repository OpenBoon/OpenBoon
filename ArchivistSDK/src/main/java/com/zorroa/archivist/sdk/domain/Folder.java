package com.zorroa.archivist.sdk.domain;

public class Folder {

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";

    private String id;
    private String parentId;
    private String name;
    private int userId;
    private AssetSearchBuilder query;
    private boolean shared;

    public Folder() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public AssetSearchBuilder getQuery() {
        return query;
    }

    public void setQuery(AssetSearchBuilder query) {
        this.query = query;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }
}
