package com.zorroa.archivist.domain;

import java.io.File;
import java.util.Map;
import java.util.List;

import org.elasticsearch.common.collect.Maps;

import com.zorroa.archivist.FileUtils;

public class AssetBuilder {

    public final Map<String, Object> document = Maps.newHashMap();
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
            map = Maps.newHashMapWithExpectedSize(16);
            document.put(namespace, map);
        }
        map.put(key,  value);
    }

    public void put(String namespace, String key, List<String> value) {
    	Map<String,Object> map = (Map<String,Object>) document.get(namespace);
    	if (map == null) {
    		map = Maps.newHashMapWithExpectedSize(16);
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
        return FileUtils.extension(file.getName());
    }

    public String getFilename() {
        return file.getName();
    }

    @Override
    public String toString() {
        return String.format("<Asset(\"%s\")>", file.getAbsolutePath());
    }
}
