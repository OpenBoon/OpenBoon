package com.zorroa.archivist.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.sdk.search.AssetSearch;

import java.util.Map;
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
    private UserBase user;
    private long timeCreated;
    private long timeModified;
    private boolean recursive;
    private boolean dyhiRoot;
    private String dyhiField;
    private int childCount;

    private Acl acl;

    private AssetSearch search;
    private boolean taxonomyRoot;
    private Map<String, Object> attrs;

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

    public UserBase getUser() {
        return user;
    }

    public Folder setUser(UserBase user) {
        this.user = user;
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

    public boolean isTaxonomyRoot() {
        return taxonomyRoot;
    }

    public Folder setTaxonomyRoot(boolean taxonomyRoot) {
        this.taxonomyRoot = taxonomyRoot;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public Folder setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("parentId", parentId)
                .add("name", name)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Folder other = (Folder) o;
        return id == other.getId();
    }

    public int getChildCount() {
        return childCount;
    }

    public Folder setChildCount(int childCount) {
        this.childCount = childCount;
        return this;
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
