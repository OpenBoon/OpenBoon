package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.schema.AttrSchema;
import com.zorroa.archivist.sdk.schema.Schema;
import com.zorroa.archivist.sdk.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.util.Map;

/**
 * A Document is wrapper around a Map<String, Object> which is used for building and interrogating
 * JSON style data.   The document is structured using Schema objects, each one stored
 * in a different namespace.
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
     * Add a new schema to the document
     *
     * @param schema
     * @return
     */
    public Document addSchema(Schema schema) {
        this.document.put(schema.getNamespace(), schema);
        return this;
    }

    /**
     * Add a new schema to the document with the given namespace.
     *
     * @param namespace
     * @param schema
     * @return
     */
    public Document addSchema(String namespace, Schema schema) {
        this.document.put(namespace, schema);
        return this;
    }

    /**
     * Return true if the document has the given namespace.
     * @param namespace
     * @return
     */
    public boolean contains(String namespace) {
        return document.containsKey(namespace);
    }

    /**
     * Get a Schema object out of the document.  A Schema is a collection of typed
     * attributes.
     *
     * @param namespace
     * @param klass
     * @param <T>
     * @return
     */
    public <T> T getSchema(String namespace, Class<T> klass) {
        return Json.Mapper.convertValue(document.get(namespace), klass);
    }

    /**
     * Get a Schema object out of the document. A Schema is a collection of typed
     * attributes.
     *
     * @param namespace
     * @param typeRef
     * @param <T>
     * @return
     */
    public <T> T getSchema(String namespace, TypeReference<T> typeRef) {
        return Json.Mapper.convertValue(document.get(namespace), typeRef);
    }

    /**
     * Retrieve an value off an internal schema.
     *
     * @param namespace
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getAttr(String namespace, String key) {
        try {
            return (T) new PropertyDescriptor(key,
                    document.get(namespace).getClass()).getReadMethod().invoke(document.get(namespace));
        } catch (Exception e) {
            try {
                Map<String,Object> schema = (Map<String,Object>) document.get(namespace);
                return (T) schema.get(key);
            }
            catch (ClassCastException ex) {
                return null;
            }
        }
    }

    /**
     * Retrieve a value of an internal schema using dot notation style, for example:
     * String path = doc.get("source.path")
     *
     * This method can return null.
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getAttr(String key) {
        if (key.contains(".")) {
            Object current = document;
            for (String e: Splitter.on('.').split(key.substring(0, key.lastIndexOf('.')))) {
                current = getObject(current, e);
                if (current == null) {
                    return null;
                }
            }
            return (T) getObject(current, key.substring(key.lastIndexOf('.')+1));
        }
        else {
            return (T) document.get(key);
        }
    }

    /**
     * Retrieve a value of an internal schema using dot notation style, for example:
     * String path = doc.get("source.path").  If the value does not exist return
     * the supplied default value.
     *
     * @param key
     * @param <T>
     * @return
     */
    public <T> T getAttrOrDefault(String key, T def) {
        if (key.contains(".")) {
            Object current = document;
            for (String e: Splitter.on('.').split(key.substring(0, key.lastIndexOf('.')))) {
                current = getObject(current, e);
                if (current == null) {
                    return def;
                }
            }
            return (T) getObject(current, key.substring(key.lastIndexOf('.')+1));
        }
        else {
            return (T) document.getOrDefault(key, def);
        }
    }

    /**
     * Set an attribute.
     *
     * @param namespace
     * @param key
     * @param values
     */
    public Document setAttr(String namespace, String key, Object[] values) {
        Object schema = this.document.get(namespace);
        if (schema == null) {
            addSchema(new AttrSchema(namespace).setAttr(key, values));
            return this;
        }

        try {
            new PropertyDescriptor(key, schema.getClass()).getWriteMethod().invoke(schema, values);
        } catch (Exception e1) {
            Map<String, Object> _schema = (Map<String, Object>) schema;
            _schema.put(key, values);
        }
        return this;
    }

    /**
     * Set an attribute.
     *
     * @param namespace
     * @param key
     * @param value
     * @return
     */
    public Document setAttr(String namespace, String key, Object value) {
        Object schema = this.document.get(namespace);
        if (schema == null) {
            addSchema(new AttrSchema(namespace).setAttr(key, value));
            return this;
        }

        try {
            new PropertyDescriptor(key, schema.getClass()).getWriteMethod().invoke(schema, value);
        } catch (Exception e1) {
            Map<String, Object> _schema = (Map<String, Object>) schema;
            _schema.put(key, value);
        }
        return this;
    }

    /**
     * Remove an attribute. This only works on arbitrary attributes, not
     * on actual schemed attributes.
     *
     * @param namespace
     * @param key
     * @return
     */
    public boolean removeAttr(String namespace, String key) {
        Object schema = this.document.get(namespace);
        if (schema == null) {
          return false;
        }

        try {
            Map<String, Object> _schema = (Map<String, Object>) schema;
            return _schema.remove(key) != null;
        } catch (Exception ignore) {

        }
        return false;
    }

    /**
     * Remove an attribute. This only works on arbitrary attributes, not
     * on actual schemed attributes.
     *
     * @param name
     * @return
     */
    public boolean removeAttr(String name) {
        if (name.contains(".")) {
            Object current = document;
            for (String e: Splitter.on('.').split(name.substring(0, name.lastIndexOf('.')))) {
                current = getObject(current, e);
                if (current == null) {
                    return false;
                }
            }

            try {
                Map<String, Object> _schema = (Map<String, Object>) current;
                return _schema.remove(name.substring(name.lastIndexOf('.')+1)) != null;
            } catch (Exception ignore) {

            }
            return false;
        }
        else {
            return document.remove(name) != null;
        }
    }


    private <T> T getObject(Object object, String key) {
        if (object == null) {
            return null;
        }
        try {
            return (T) new PropertyDescriptor(key,
                    object.getClass()).getReadMethod().invoke(object);
        } catch (Exception e) {
            try {
                return (T) ((Map<String,Object>)object).get(key);
            }
            catch (ClassCastException ex) {
                return null;
            }
        }
    }
}
