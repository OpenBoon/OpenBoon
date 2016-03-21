package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * A Document is a wrapper around a Map<String,Object> which provides a convenience interface
 * to the underlying structure.
 */
public class Document {

    private static final Logger logger = LoggerFactory.getLogger(Document.class);

    protected  Map<String, Object> document = Maps.newHashMap();

    public Map<String, Object> getDocument() {
        return document;
    }

    public void setDocument(Map<String, Object> data) {
        this.document = data;
    }

    /**
     * Get an attribute value  by its fully qualified name.
     *
     * @param attr
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr) {
        Object current = getContainer(attr, false);
        return (T) getChild(current, Attr.name(attr));
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified class.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr, Class<T> type) {
        Object current = getContainer(attr, false);
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type);
    }

    /**
     * Get an attibute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified TypeReference.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
     */
    public <T> T getAttr(String attr, TypeReference<T> type) {
        Object current = getContainer(attr, false);
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type);
    }

    /**
     * Set an attribute value.
     *
     * @param attr
     * @param value
     */
    public void setAttr(String attr, Object value) {
        Object current = getContainer(attr, true);
        String key = Attr.name(attr);

        try {
            /*
             * Try to use an exposed setter method.
             */
            new PropertyDescriptor(key,
                    current.getClass()).getWriteMethod().invoke(current, value);
            logger.info("setter was called for key {}", key);
        } catch (Exception e) {

            /*
             * If the setter doesn't exist, try to use the any setter.
             */
            for (Method m : current.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnySetter.class)) {
                    try {
                        m.invoke(current, key, value);
                        return;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any setter call: " + attr + " value: " + value, e);
                    }
                }
            }

            /*
             * Finally, try treating it like a map.
             */
            try {
                ((Map<String, Object>) current).put(key, value);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + attr);
            }
        }
    }

    /**
     * Return true if the document has the given namespace.
     * @param attr
     * @return
     */
    public boolean attrExists(String attr) {
        Object container = getContainer(attr, false);
        return getChild(container, Attr.name(attr)) != null;
    }

    private Object getContainer(String attr, boolean forceExpand) {
        String[] parts = attr.split(Attr.DELIMITER);
        Object current = document;
        for (int i=0; i<parts.length-1; i++) {
            Object child = getChild(current, parts[i]);
            if (child == null) {
                if (forceExpand) {
                    child = createChild(current, parts[i]);
                }
                else {
                    return null;
                }
            }
            current = child;
        }
        return current;
    }

    private Object getChild(Object object, String key) {
        if (object == null) {
            return null;
        }
        try {

            return new PropertyDescriptor(key,
                    object.getClass()).getReadMethod().invoke(object);
        } catch (Exception e) {
            /*
             * If the setter doesn't exist, try to use the any setter.
             */
            for (Method m : object.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnyGetter.class)) {
                    try {
                        return ((Map)m.invoke(object)).get(key);
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid any getter call: " + key);
                    }
                }
            }

            try {
                return ((Map<String, Object>) object).get(key);
            } catch (ClassCastException ex) {
                return null;
            }
        }
    }

    private Object createChild(Object parent, String key) {
        Map<String, Object> result = Maps.newHashMap();
        try {
            new PropertyDescriptor(key,
                    parent.getClass()).getWriteMethod().invoke(parent, result);
        } catch (Exception e) {

            for (Method m : parent.getClass().getMethods()) {
                if (m.isAnnotationPresent(JsonAnySetter.class)) {
                    try {
                        m.invoke(parent, key, result);
                        // early return
                        return result;
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Invalid attribute: " + key + " parent: " + parent);
                    }
                }
            }

            try {
                ((Map<String, Object>) parent).put(key, result);
            } catch (ClassCastException ex) {
                throw new IllegalArgumentException("Invalid attribute: " + key + " parent: " + parent);
            }
        }
        return result;
    }
}

