package com.zorroa.zmlp.sdk.domain.Asset;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores an Asset search, is modeled after an ElasticSearch request.
 */
public class AssetSearch {

    /**
     * The search to execute
     */
    private Map<String, Object> search;

    /**
     * A query to execute on nested elements.
     */
    private Map<String, Object> elementQuery;

    public AssetSearch() {
        search = new HashMap();
        elementQuery = new HashMap();
    }

    public AssetSearch(Map<String, Object> search, Map<String, Object> elementQuery) {
        this.search = search;
        this.elementQuery = elementQuery;
    }

    public AssetSearch(Map<String, Object> search) {
        this.search = search;
        this.elementQuery = new HashMap();
    }

    public Map<String, Object> getSearch() {
        return search;
    }

    public void setSearch(Map<String, Object> search) {
        this.search = search;
    }

    public Map<String, Object> getElementQuery() {
        return elementQuery;
    }

    public void setElementQuery(Map<String, Object> elementQuery) {
        this.elementQuery = elementQuery;
    }
}


