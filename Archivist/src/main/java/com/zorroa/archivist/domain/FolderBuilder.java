package com.zorroa.archivist.domain;

import java.util.HashMap;
import java.util.Map;

public class FolderBuilder {
    private String parentId;
    private String name;
    private int userId;
    private String query;

    // For the JSON MAPPER we need a simple ctor
    private FolderBuilder() {}

    // Name and userId are required arguments
    public FolderBuilder(String name, int userId) {
        this.name = name;
        this.userId = userId;
    }

    public FolderBuilder(String name, int userId, String parentId) {
        this.parentId = parentId;
        this.name = name;
        this.userId = userId;
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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getDocument() {
        HashMap<String, Object> doc = new HashMap<String, Object>();
        doc.put("name", name);
        doc.put("userId", userId);
        if (query != null)
            doc.put("query", query);
        if (parentId != null)
            doc.put("parentId", parentId);
        return doc;
    }
}
