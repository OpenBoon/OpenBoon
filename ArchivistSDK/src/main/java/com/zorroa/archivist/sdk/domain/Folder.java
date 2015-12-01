package com.zorroa.archivist.sdk.domain;

import java.util.Objects;

public class Folder {

    public static final String ROOT_ID = "00000000-0000-0000-0000-000000000000";

    /**
     * Return true if the given folder ID is the root folder's ID.
     * @param id
     * @return
     */
    public static boolean isRoot(String id) {
        return ROOT_ID.equals(id);
    }

    /**
     * Return true if the given folder is the root folder.
     * @param folder
     * @return
     */
    public static boolean isRoot(Folder folder) {
        return ROOT_ID.equals(folder.getId());
    }

    private String id;
    private String parentId;
    private String name;
    private int userCreated;
    private int userModified;
    private AssetSearch search;

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

    public AssetSearch getSearch() {
        return search;
    }

    public void setSearch(AssetSearch search) {
        this.search = search;
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
        return String.format("<Folder id=%s parent=%s name=%s>",
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
