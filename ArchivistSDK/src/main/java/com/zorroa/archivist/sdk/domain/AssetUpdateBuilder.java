package com.zorroa.archivist.sdk.domain;

import java.util.HashMap;
import java.util.Map;

public class AssetUpdateBuilder {

    private Map<String, Object> source = new HashMap<String, Object>();

    public Map<String, Object> getSource() {
        return source;
    }

    public void put(String namespace, String key, Object value) {
        Map<String, Object> map = (Map<String, Object>) source.get(namespace);
        if (map == null) {
            map = new HashMap<String, Object>(16);
            source.put(namespace, map);
        }
        map.put(key, value);
    }
}
