package com.zorroa.archivist.sdk.domain;

import java.util.Objects;

public class Folder {

    public static final Integer ROOT_ID = 0;

    /**
     * Return true if the given folder ID is the root folder's ID.
     * @param id
     * @return
     */
    public static boolean isRoot(int id) {
        return ROOT_ID == id;
    }

    /**
     * Return true if the given folder is the root folder.
     * @param folder
     * @return
     */
    public static boolean isRoot(Folder folder) {
        return ROOT_ID == folder.getId();
    }

    private Integer id;
    private Integer parentId;
    private String name;
    private int userCreated;
    private int userModified;
    private long timeCreated;
    private long timeModified;
    private boolean recursive;

    private AssetSearch search;

    public Folder() { }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
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

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public long getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(long timeModified) {
        this.timeModified = timeModified;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
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
        return id.intValue() == other.id.intValue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id.intValue());
    }
}
