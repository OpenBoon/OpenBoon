package com.zorroa.archivist.sdk.domain;

public class FolderBuilder {

    private Integer parentId = Folder.ROOT_ID;
    private String name;
    private AssetSearch search;
    private Acl acl;

    // For the JSON MAPPER we need a simple ctor
    public FolderBuilder() {}

    public FolderBuilder(Folder folder) {
        this.parentId =  folder.getParentId();
        this.name = folder.getName();
        this.search = folder.getSearch();
    }

    // Name and userId are required arguments
    public FolderBuilder(String name) {
        this.name = name;
    }

    public FolderBuilder(String name, int parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public FolderBuilder(String name, Folder parent) {
        this.name = name;
        this.parentId = parent.getId();
    }

    public int getParentId() {
        return parentId;
    }

    public FolderBuilder setParentId(int parentId) {
        this.parentId = parentId;
        return this;
    }

    public String getName() {
        return name;
    }

    public FolderBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public FolderBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public FolderBuilder setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }
}
