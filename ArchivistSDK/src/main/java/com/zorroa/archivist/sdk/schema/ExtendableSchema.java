package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Inheriting from this Schema type will allow users to set arbitrary values on the object which
 * are not defined in the subclass.  This allows you to add new fields at will, without recompiling,
 * but without compile time type safety protections.
 */
public class ExtendableSchema {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<String, Object> delegate;

    public ExtendableSchema() {
        this.delegate = Maps.newHashMap();
    }

    @JsonAnyRemover
    public boolean remove(String key) {
        return delegate.remove(key) != null;
    }

    @JsonAnyGetter
    public Map<String, Object> any() {
        return delegate;
    }

    /**
     * Set an arbitrary attribute value.
     *
     * @param name
     * @param value
     */
    @JsonAnySetter
    public void setAttr(String name, Object value) {
        try {
            /*
             * First check if there is already a field declared with the same name.  If
             * there is, we'll try to set that.  Otherwise the resulting JSON document
             * will have 2 keys with the same name.
             */
            Field field = getClass().getDeclaredField(name);
            field.set(this, value);
        } catch (NoSuchFieldException e) {
            delegate.put(name, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to set value of field: " + name + "," + e, e);
        }
    }

    /**
     * Get the value of an arbitrary attribute.
     * @param name
     * @param <T>
     * @return
     */
    public <T> T getAttr(String name) {
        return (T) delegate.get(name);
    }
}
