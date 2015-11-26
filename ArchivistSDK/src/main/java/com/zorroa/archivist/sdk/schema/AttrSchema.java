package com.zorroa.archivist.sdk.schema;

import java.util.HashMap;

/**
 * A general purpose schema.
 */
public class AttrSchema extends HashMap<String, Object> implements Schema {

    private final String namespace;

    public AttrSchema(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setAttr(String name, Object value) {
        this.put(name, value);
    }

    public <T> T setAttr(String key) {
        return (T) this.get(key);
    }
}
