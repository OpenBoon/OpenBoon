package com.zorroa.archivist.sdk.domain;

public class FolderBuilder {

    private String parentId = Folder.ROOT_ID;
    private String name;
    private AssetSearchBuilder query;

    // For the JSON MAPPER we need a simple ctor
    public FolderBuilder() {}

    public FolderBuilder(Folder folder) {
        this.parentId =  folder.getParentId();
        this.name = folder.getName();
        this.query = folder.getQuery();
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

    public AssetSearchBuilder getQuery() {
        return query;
    }

    public FolderBuilder setQuery(AssetSearchBuilder query) {
        this.query = query;
        return this;
    }
}
