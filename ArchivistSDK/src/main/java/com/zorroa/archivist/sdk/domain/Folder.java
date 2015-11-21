package com.zorroa.archivist.sdk.domain;

import java.util.Objects;

public class Folder {

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";
    public static boolean isRoot(String id) {
        return ROOT_ID.equals(id);
    }

    private String id;
    private String parentId;
    private String name;
    private int userCreated;
    private int userModified;
    private AssetSearchBuilder query;

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


    public AssetSearchBuilder getQuery() {
        return query;
    }

    public void setQuery(AssetSearchBuilder query) {
        this.query = query;
    }

    public int getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(int userCreated) {
        this.userCreated = userCreated;
    }

    public int getUserModified() {
        return userModified;
    }

    public void setUserModified(int userModified) {
        this.userModified = userModified;
    }

    @Override
    public String toString() {
        return String.format("<Folder id=%s parent=%s name=%s shared=%s>",
                id, parentId, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder other = (Folder) o;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
