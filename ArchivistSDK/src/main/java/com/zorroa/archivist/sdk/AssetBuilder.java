package com.zorroa.archivist.sdk;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetBuilder {

    public final Map<String, Object> document = new HashMap<String, Object>();
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

    public void put(String namespace, String key, Object value) {
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            map = new HashMap<String, Object>(16);
            document.put(namespace, map);
        }
        map.put(key,  value);
    }

    public void put(String namespace, String key, List<String> value) {
    	Map<String,Object> map = (Map<String,Object>) document.get(namespace);
    	if (map == null) {
    		map = new HashMap<String, Object>(16);
    		document.put(namespace, map);
    	}
    	map.put(key, value);
    }

    public void put(String namespace, Map<String, Object> value) {
        Map<String,Object> map = (Map<String,Object>) document.get(namespace);
        if (map == null) {
            document.put(namespace, value);
        }
        else {
            map.putAll(value);
        }
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

    public boolean isImageType() {
        return true;    // TODO: Check the list of supported image formats?
    }

    @Override
    public String toString() {
        return String.format("<Asset(\"%s\")>", file.getAbsolutePath());
    }
}
