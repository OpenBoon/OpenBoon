package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by chambers on 1/5/16.
 */
public class FolderUpdateBuilder {

    private Integer parentId;
    private String name;
    private AssetSearch search;
    private Acl acl;
    private Set<String> isset;

    public FolderUpdateBuilder() {
        isset = Sets.newHashSet();
    }

    public Integer getParentId() {
        return parentId;
    }

    public FolderUpdateBuilder setParentId(Integer parentId) {
        this.parentId = parentId;
        isset.add("parentId");
        return this;
    }

    public String getName() {
        return name;
    }

    public FolderUpdateBuilder setName(String name) {
        this.name = name;
        isset.add("name");
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public FolderUpdateBuilder setSearch(AssetSearch search) {
        this.search = search;
        isset.add("search");
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public FolderUpdateBuilder setAcl(Acl acl) {
        this.acl = acl;
        isset.add("acl");
        return this;
    }

    public Set<String> getIsset() {
        return isset;
    }

    public FolderUpdateBuilder setIsset(Set<String> isset) {
        this.isset = isset;
        return this;
    }

    public boolean isset(String name) {
        return isset.contains(name);
    }
}
