package com.zorroa.archivist.domain;

import com.zorroa.sdk.domain.Acl;
import com.zorroa.sdk.domain.AssetSearch;
import com.zorroa.sdk.domain.EventLoggable;

import java.util.Objects;

public class Folder implements EventLoggable {

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

    private Acl acl;

    private AssetSearch search;

    public Folder() { }

    public int getId() {
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

    public String getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(String userCreated) {
        this.userCreated = userCreated;
    }

    public String getUserModified() {
        return userModified;
    }

    public void setUserModified(String userModified) {
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

    @Override
    public String toString() {
        return String.format("<Folder id=%s parent=%s name=%s acl=%s>",
                id, parentId, name, acl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        com.zorroa.sdk.domain.Folder other = (com.zorroa.sdk.domain.Folder) o;
        return id == other.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public Object getLogId() {
        return id;
    }
}
