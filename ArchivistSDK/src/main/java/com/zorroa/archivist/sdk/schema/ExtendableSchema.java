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
public  class ExtendableSchema<K, V> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Map<K, V> delegate;

    public ExtendableSchema() {
        this.delegate = Maps.newHashMap();
    }

    /**
     * Called by Document class when a new instance of V is necessary to
     * dynamically build the docuument.  This is usually only needed if
     * V a type of Collection or Map.
     *
     * @return
     */
    public V getDefaultValue() {
        return null;
    }

    @JsonAnyRemover
    public boolean remove(String key) {
        return delegate.remove(key) != null;
    }

    @JsonAnyGetter
    public Map<K, V> any() {
        return delegate;
    }

    /**
     * Set an arbitrary attribute value.
     *
     * @param name
     * @param value
     */
    @JsonAnySetter
    public void setAttr(K name, V value) {
        try {
            /*
             * First check if there is already a field declared with the same name.  If
             * there is, we'll try to set that.  Otherwise the resulting JSON document
             * will have 2 keys with the same name.
             */
            Field field = getClass().getDeclaredField(name.toString());
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
