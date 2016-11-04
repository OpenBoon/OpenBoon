package com.zorroa.archivist.domain;

import com.zorroa.sdk.search.AssetSearch;

import java.util.Objects;

public class Folder implements Loggable<Integer> {

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

    private int id;
    private Integer parentId;
    private Integer dyhiId;
    private String name;
    private String userCreated;
    private String userModified;
    private long timeCreated;
    private long timeModified;
    private boolean recursive;
    private boolean dyhiRoot;
    private String dyhiField;

    private Acl acl;

    private AssetSearch search;

    public Folder() { }

    public int getId() {
        return id;
    }

    public Folder setId(int id) {
        this.id = id;
        return this;
    }

    public Integer getParentId() {
        return parentId;
    }

    public Folder setParentId(Integer parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public Folder setName(String name) {
        this.name = name;
        return this;
    }

    public String getUserCreated() {
        return userCreated;
    }

    public Folder setUserCreated(String userCreated) {
        this.userCreated = userCreated;
        return this;
    }

    public String getUserModified() {
        return userModified;
    }

    public Folder setUserModified(String userModified) {
        this.userModified = userModified;
        return this;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public Folder setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public long getTimeModified() {
        return timeModified;
    }

    public Folder setTimeModified(long timeModified) {
        this.timeModified = timeModified;
        return this;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public Folder setRecursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public Folder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Integer getDyhiId() {
        return dyhiId;
    }

    public Folder setDyhiId(Integer dyhiId) {
        this.dyhiId = dyhiId;
        return this;
    }

    public boolean isDyhiRoot() {
        return dyhiRoot;
    }

    public Folder setDyhiRoot(boolean dyhiRoot) {
        this.dyhiRoot = dyhiRoot;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public Folder setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public String getDyhiField() {
        return dyhiField;
    }

    public Folder setDyhiField(String dyhiField) {
        this.dyhiField = dyhiField;
        return this;
    }

    @Override
    public String toString() {
        return String.format("<Folder id=%s parent=%s name=%s acl=%s>",
                id, parentId, name, acl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder other = (Folder) o;
        return id == other.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }


    @Override
    public Integer getTargetId() {
        return getId();
    }
}
