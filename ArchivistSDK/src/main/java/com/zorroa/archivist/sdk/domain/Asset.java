package com.zorroa.archivist.sdk.domain;

import com.google.common.base.Splitter;

import java.util.Map;

public class Asset {

    private String id;
    private long version;
    private Map<String,Object> document;

    public Asset() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Hard coded for now, this should be set by ingests.
     * Need schema feature here.
     *
     * @return
     */
    public AssetType getType() {
        return AssetType.Image;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> data) {
        this.document = data;
    }

    public <T> T getValue(String key) {
        if (key.contains(".")) {
            Map<String, Object> map = getMap(key.substring(0, key.lastIndexOf('.')));
            return (T) map.get(key.substring(key.lastIndexOf('.')+1));
        }
        else {
            return (T) document.get(key);
        }
    }

    private Map<String,Object> getMap(String key) {
        // TODO: this might not always be a map.
        Map<String,Object> current = null;
        for (String e: Splitter.on('.').split(key)) {
            current = (Map<String,Object>) document.get(e);
        }
        return current;
    }
}
