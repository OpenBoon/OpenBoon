package com.zorroa.archivist.domain;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;

public class AssetBuilder {

    public Map<String, Object> document = Maps.newHashMap();

    public AssetBuilder() {
        // TODO Auto-generated constructor stub
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> document) {
        this.document = document;
    }

    public void put(String namespace, String key, Object value) {

        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            map = Maps.newHashMapWithExpectedSize(16);
            document.put(namespace, map);
        }
        map.put(key,  value);
    }

}
