package com.zorroa.archivist.sdk.schema;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * A general purpose schema.
 */
public class AttrSchema extends ForwardingMap<String, Object> implements Schema {

    private final String namespace;
    private final Map<String, Object> delegate;

    public AttrSchema(String namespace) {
        this.namespace = namespace;
        this.delegate = Maps.newHashMap();
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

    @Override
    protected Map<String, Object> delegate() {
        return delegate;
    }
}
