package com.zorroa.archivist.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Objects;

public class FolderSpec {

    @NotNull
    private Integer parentId;

    @NotEmpty
    private String name;

    private Integer dyhiId;

    private AssetSearch search;

    private Acl acl;

    private Map<String, Object> attrs;

    @JsonIgnore
    public boolean created = false;
    /**
     * Only used locally, don't accept over the wire.
     */
    @JsonIgnore
    private Integer userId;

    /**
     * A smart folder search recurses into all child folders.
     */
    private boolean recursive = true;

    // For the JSON MAPPER we need a simple ctor
    public FolderSpec() {}

    public FolderSpec(Folder folder) {
        this.parentId =  folder.getParentId();
        this.name = folder.getName();
        this.search = folder.getSearch();
    }

    // Name and userId are required argument
    public FolderSpec(String name) {
        this.parentId = Folder.ROOT_ID;
        this.name = name;
    }

    public FolderSpec(String name, int parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public FolderSpec(String name, Folder parent) {
        this.name = name;
        this.parentId = parent.getId();
    }

    public Integer getParentId() {
        return parentId;
    }

    public FolderSpec setParentId(Integer parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public FolderSpec setName(String name) {
        this.name = name;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public FolderSpec setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public FolderSpec setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public FolderSpec setRecursive(boolean recursive) {
        this.recursive = recursive;
        return this;
    }

    public Integer getDyhiId() {
        return dyhiId;
    }

    public FolderSpec setDyhiId(Integer dyhiId) {
        this.dyhiId = dyhiId;
        return this;
    }

    public Map<String, Object> getAttrs() {
        return attrs;
    }

    public FolderSpec setAttrs(Map<String, Object> attrs) {
        this.attrs = attrs;
        return this;
    }

    public Integer getUserId() {
        return userId;
    }

    public FolderSpec setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("parentId", parentId)
                .add("name", name)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FolderSpec that = (FolderSpec) o;
        return Objects.equals(getParentId(), that.getParentId()) &&
                Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParentId(), getName());
    }
}
