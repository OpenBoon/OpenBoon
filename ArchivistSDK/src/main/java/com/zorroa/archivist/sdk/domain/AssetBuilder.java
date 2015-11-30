package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Point2D;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public class AssetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AssetBuilder.class);

    /**
     * Currenty the main aggregation point for all the asset data.
     */
    private final Map<String, Object> document = new HashMap<>();

    /**
     * We're using dynamic mapping templates for this now.
     */
    @Deprecated
    private final Map<String, Object> mapping = new HashMap<>();

    /**
     * Eventually this will replace document, but for now we add schema objects
     * both here, and to document.
     */
    private final Map<String, Schema> schemas = new HashMap<>();

    private boolean async = false;
    private final File file;

    /**
     * The SourceSchema describes all the common, non-format specific, source
     * file information.  Mainly this is file path info and asset type.
     */
    private SourceSchema source;

    /**
     * This schema describes all the different keywords fields.
     */
    private KeywordsSchema keywords;

    /**
     * This schema describes the available permissions fields.
     */
    private PermissionSchema permissions;

    public AssetBuilder(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException(
                "AssetBuilder must point to a regular file.");
        }

        this.file = file;

        /*
         * Add the standard schemas.
         */

        source = new SourceSchema();
        source.setBasename(getBasename());
        source.setDirectory(getDirectory());
        source.setExtension(getExtension());
        source.setPath(getAbsolutePath());
        source.setExtension(getExtension());
        source.setFilename(getFilename());
        addSchema(source);

        keywords = new KeywordsSchema();
        addSchema(keywords);

        permissions = new PermissionSchema();
        addSchema(permissions);

        switch(source.getType()) {
            case Image:
                addSchema(new ImageSchema());
                break;
            default:
                logger.warn("Unsupported asset type: '%s'", source.getType());
        }


    }

    public AssetBuilder(String file) {
        this(new File(file));
    }

    /*
     * NEW API
     */
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    public AssetBuilder addSchema(Schema schema) {
        this.schemas.put(schema.getNamespace(), schema);
        this.document.put(schema.getNamespace(), schema);
        return this;
    }

    public <T> T getSchema(String namespace, Class<T> type) {
        return (T) schemas.get(namespace);
    }

    public <T> T getSchema(String namespace) {
        return (T) schemas.get(namespace);
    }

    public <T> T getAttr(String namespace, String key) {
        try {
            return (T) new PropertyDescriptor(key,
                    schemas.get(namespace).getClass()).getReadMethod().invoke(schemas.get(namespace));
        } catch (Exception e) {
            return null;
        }

    }
    public AssetBuilder addKeywords(double confidence, boolean suggest, String ... words) {
        keywords.addKeywords(confidence, suggest, words);
        return this;
    }

    public AssetBuilder addKeywords(double confidence, boolean suggest, Collection<String> words) {
        keywords.addKeywords(confidence, suggest, words.toArray(new String[] {}));
        return this;
    }

    public SourceSchema getSource() {
        return source;
    }

    public KeywordsSchema getKeywords() {
        return keywords;
    }

    public void setAttr(String namespace, String key, Object[] values) {
        AttrSchema schema = (AttrSchema) this.schemas.get(namespace);
        if (schema == null) {
            schema = new AttrSchema(namespace);
            addSchema(schema);
        }
        schema.setAttr(key, values);
    }


    public void setAttr(String namespace, String key, Object value) {
        AttrSchema schema = (AttrSchema) this.schemas.get(namespace);
        if (schema == null) {
            schema = new AttrSchema(namespace);
            addSchema(schema);
        }
        schema.setAttr(key, value);
    }

    public boolean isType(AssetType type) {
        return getSource().getType().equals(type);
    }

    /**
     * This method copies all schema properties annotated with Keyword
     * to the proper Keywords bucket.  This is called right before the asset
     * is added to ElasticSearch.
     */
    public void buildKeywords() {
        KeywordsSchema keywords = getKeywords();
        for (Schema s: schemas.values()) {
            for (Field field : s.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Keyword annotation = field.getAnnotation(Keyword.class);
                if (annotation == null) {
                    continue;
                }
                try {
                    keywords.addKeywords(annotation.confidence(), false, field.get(s).toString());
                } catch (IllegalAccessException e) {
                    logger.warn("Failed to access {}, ", field, e);
                }
            }
        }
    }

    /*
     * OLD API
     */

    public Map<String, Object> getDocument() {
        return document;
    }

    public File getFile() {
        return file;
    }

    public String getDirectory() {
        return file.getParent();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
    }

    public void setSearchPermissions(Permission ... perms) {
        permissions.getSearch().clear();
        for (Permission p: perms) {
            permissions.getSearch().add(p.getId());
        }
    }

    public void setExportPermissions(Permission ... perms) {
        permissions.getExport().clear();
        for (Permission p: perms) {
            permissions.getExport().add(p.getId());
        }
    }

    public String getBasename() {
        String path = file.getName();
        try {
            return path.substring(0, path.lastIndexOf('.')).toLowerCase();
        } catch (IndexOutOfBoundsException ignore) { /*EMPTY*/ }
        return "";
    }

    public String getExtension() {
        String path = file.getName();
        try {
            return path.substring(path.lastIndexOf('.') + 1).toLowerCase();
        } catch (IndexOutOfBoundsException ignore) { /*EMPTY*/ }
        return "";
    }

    public String getFilename() {
        return file.getName();
    }

    // Get a value for the specified namespace.key
    public Object get(String namespace, String key) {
        Map<String, Object> map = (Map<String, Object>) document.get(namespace);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    // Controls whether the Asset is created asynchronously
    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    // Only store valid ES values
    private boolean isValidValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof String && ((String)value).length() > 32765) {
            return false;
        }
        if (value.getClass().isArray() && Array.getLength(value) > 32765) {
            return false;
        }
        return true;
    }

    // Insert a generic object into the document.
    // Returns true if added, and false if the value is invalid.
    public boolean put(String namespace, String key, Object value) {
        if (!isValidValue(value)) {
            return false;
        }
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            map = new HashMap<String, Object>(16);
            document.put(namespace, map);
        }
        map.put(key,  value);
        return true;
    }

    // Insert an arbitrary map of values for the namespace, replacing
    // any existing values for this namespace.
    public boolean put(String namespace, Map<String, Object> value) {
        if (!isValidValue(value)) {
            return false;
        }
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            document.put(namespace, value);
        } else {
            map.putAll(value);
        }
        return true;
    }

    // Create a multi-field string mapping with a analyzed and .raw values
    private void mapString(String namespace, String key) {
        map(namespace, key, "type", "string");
        HashMap<String, String> raw = new HashMap<String, String>(2);
        raw.put("type", "string");
        raw.put("index", "not_analyzed");
        HashMap<String, Object> fields = new HashMap<String, Object>(2);
        fields.put("raw", raw);
        map(namespace, key, "fields", fields);
    }

    // Insert a string type, creating both a analyzed and .raw values
    public boolean put(String namespace, String key, String value) {
        if (!put(namespace, key, (Object)value)) {
            return false;
        }
        mapString(namespace, key);
        return true;
    }

    // Insert an array of strings, with both analyzed and .raw values
    public boolean put(String namespace, String key, String[] value) {
        if (!put(namespace, key, (Object)value)) {
            return false;
        }
        mapString(namespace, key);
        return true;
    }

    // Insert a date value as an ES date type
    public boolean put(String namespace, String key, Date date) {
        if (!put(namespace, key, (Object)date)) {
            return false;
        }
        map(namespace, key, "type", "date");
        return true;
    }

    // Insert a geoPoint ES type, important to use Point2D.double for accuracy
    public void put(String namespace, String key, Point2D location) {
        Map<String, Object> geoPoint = new HashMap<String, Object>(2);
        geoPoint.put("lat", location.getX());
        geoPoint.put("lon", location.getY());
        if (put(namespace, key, geoPoint)) {
            map(namespace, key, "type", "geo_point");
        }
    }

    // Create a keyword string mapping which copies the string to the
    // generic index used for searches and suggestions.
    private void mapKeyword(String namespace, String key) {
        mapString(namespace, key);
        map(namespace, key, "copy_to", null);
    }

    // Insert a keyword string that will be found by generic searches
    public void putKeyword(String namespace, String key, String keyword) {
        if (put(namespace, key, keyword)) {
            mapKeyword(namespace, key);
        }
    }

    // Insert an array of keyword strings that will be found by generic searches
    public void putKeywords(String namespace, String key, String[] keywords) {
        if (put(namespace, key, keywords)) {
            mapKeyword(namespace, key);
        }
    }

    // Remove an entry from the document
    public Object remove(String namespace, String key) {
        Map<String, Object> map = (Map<String, Object>) document.get(namespace);
        if (map != null) {
            return map.remove(key);
        }
        return null;
    }

    // Return the full ES type mapping for the document
    public Map<String, Object> getMapping() {
        return mapping;
    }

    // Create a mapping for the document, e.g. map(namespace, key, "type", "date"),
    // or map(namespace, key, "copy_to", ["keywords", "keywords_suggest"]).
    // Passing value=null for option="copy_to" copies to default search field.
    // Note that a "copy_to" mapping must also have a "type" mapping!
    private void map(String namespace, String key, String option, Object value) {
        // Create a new entry in the local mapping
        Map<String,Object> directory = (Map<String,Object>) mapping.get(namespace);
        if (directory == null) {
            directory = new HashMap<String, Object>(16);
            mapping.put(namespace, directory);
        }
        Map<String, Object> field = (Map<String, Object>) directory.get(key);
        if (field == null) {
            field = new HashMap<String, Object>(2);
            directory.put(key, field);
        }

        // Provide a default copy_to value -> ["keywords", "keywords_suggest"]
        if (value == null && option.equals("copy_to")) {
            List<String> copyToKeywords = new ArrayList<String>();
            copyToKeywords.add("keywords.indexed");
            copyToKeywords.add("keywords.untouched");
            copyToKeywords.add("keywords_suggest");
            value = copyToKeywords;
        }
        field.put(option, value);
    }

    @Override
    public String toString() {
        return String.format("<Asset(\"%s\")>", file.getAbsolutePath());
    }
}
