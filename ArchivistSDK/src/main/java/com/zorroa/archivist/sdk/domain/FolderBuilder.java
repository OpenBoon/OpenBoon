package com.zorroa.archivist.sdk.domain;

public class FolderBuilder {

    private String parentId = Folder.ROOT_ID;
    private String name;
    private AssetSearch search;

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

    public FolderBuilder(String name, String parentId) {
        this.name = name;
        this.parentId = parentId;
    }

    public FolderBuilder(String name, Folder parent) {
        this.name = name;
        this.parentId = parent.getId();
    }

    public String getParentId() {
        return parentId;
    }

    public FolderBuilder setParentId(String parentId) {
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
}
