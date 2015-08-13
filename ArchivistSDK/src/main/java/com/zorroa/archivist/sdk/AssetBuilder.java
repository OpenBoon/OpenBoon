package com.zorroa.archivist.sdk;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AssetBuilder {

    public final Map<String, Object> document = new HashMap<String, Object>();
    public final Map<String, Object> mapping = new HashMap<String, Object>();
    private static final Map<String, Object> _mapped = new ConcurrentHashMap<String, Object>();
    private boolean async = false;
    private final File file;

    public AssetBuilder(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException(
                "AssetBuilder must point to a regular file.");
        }

        this.file = file;
        this.put("source", "filename", this.getFilename());
        this.put("source", "directory", this.getDirectory());
        this.put("source", "path", this.getAbsolutePath());
        this.put("source", "extension", this.getExtension());
    }

    public AssetBuilder(String file) {
        this(new File(file));
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public File getFile() {
        return file;
    }

    private boolean isValidValue(Object value) {
        if (value instanceof String && ((String)value).length() > 32765) {
            return false;
        } else if (value.getClass().isArray() && Array.getLength(value) > 32765) {
            return false;
        }
        return true;
    }

    public void put(String namespace, String key, Object value) {
        if (!isValidValue(value)) {
            return;
        }
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            map = new HashMap<String, Object>(16);
            document.put(namespace, map);
        }
        map.put(key,  value);
    }

    public void put(String namespace, String key, List<String> value) {
        if (!isValidValue(value)) {
            return;
        }
    	Map<String,Object> map = (Map<String,Object>) document.get(namespace);
    	if (map == null) {
    		map = new HashMap<String, Object>(16);
    		document.put(namespace, map);
    	}
    	map.put(key, value);
    }

    public void put(String namespace, Map<String, Object> value) {
        if (!isValidValue(value)) {
            return;
        }
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            document.put(namespace, value);
        }
        else {
            map.putAll(value);
        }
    }

    public Object remove(String namespace, String key) {
        Map<String, Object> map = (Map<String, Object>) document.get(namespace);
        if (map != null) {
            return map.remove(key);
        }
        return null;
    }

    public Map<String, Object> getMapping() {
        return mapping;
    }

    // Update the static mapped field, after ES mapping has been updated
    public void updateMapped() {
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            String namespaceKey = entry.getKey();
            Map<String, Object> namespaceMap = (Map<String, Object>) entry.getValue();
            Map<String, Object> mnamespace = (Map<String, Object>) _mapped.get(namespaceKey);
            if (mnamespace == null) {
                mnamespace = new ConcurrentHashMap<String, Object>();
                _mapped.put(namespaceKey, mnamespace);
            }
            mnamespace.putAll(namespaceMap);
        }
    }

    // Return true if the field has already been mapped in any way
    private static boolean mapped(String namespace, String key) {
        Map<String, Object> map = (Map<String, Object>) _mapped.get(namespace);
        if (map == null) {
            return false;
        }
        return map.get(key) != null;
    }

    // Create a mapping for the document, e.g. map(namespace, key, "type", "date"),
    // or map(namespace, key, "copy_to", ["keywords", "keywords_suggest"]).
    // Passing value=null for option="copy_to" copies to default search field.
    // Note that a "copy_to" mapping must also have a "type" mapping!
    public void map(String namespace, String key, String option, Object value) {
        if (mapped(namespace, key)) {       // Previously mapped?
            return;
        }

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
            copyToKeywords.add("keywords");
            copyToKeywords.add("keywords_suggest");
            value = copyToKeywords;
        }
        field.put(option, value);
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public String getDirectory() {
        return file.getParent();
    }

    public String getAbsolutePath() {
        return file.getAbsolutePath();
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

    @Override
    public String toString() {
        return String.format("<Asset(\"%s\")>", file.getAbsolutePath());
    }
}
