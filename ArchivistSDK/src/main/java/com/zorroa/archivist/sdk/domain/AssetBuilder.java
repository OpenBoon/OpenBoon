package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.archivist.sdk.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AssetBuilder {

    private static final Logger logger = LoggerFactory.getLogger(AssetBuilder.class);

    /**
     * Contains the entire JSON document
     */
    private final Map<String, Object> document = new HashMap<>();

    /**
     * The file.
     */
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

    /**
     * The existing asset.  If none exists it will be null.
     */
    private Asset previousVersion;

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
    }

    public AssetBuilder(String file) {
        this(new File(file));
    }

    public Map<String, Object> getDocument() {
        return document;
    }

    public AssetBuilder addSchema(Schema schema) {
        this.document.put(schema.getNamespace(), schema);
        return this;
    }

    public <T> T getSchema(String namespace, Class<T> type) {
        return (T) document.get(namespace);
    }

    public <T> T getSchema(String namespace) {
        return (T) document.get(namespace);
    }

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

    public void setAttr(String namespace, String key, Object value) {
        AttrSchema schema = (AttrSchema) this.document.get(namespace);
        if (schema == null) {
            schema = new AttrSchema(namespace);
            addSchema(schema);
        }
        schema.setAttr(key, value);
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
        AttrSchema schema = (AttrSchema) this.document.get(namespace);
        if (schema == null) {
            schema = new AttrSchema(namespace);
            addSchema(schema);
        }
        schema.setAttr(key, values);
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
        for (Object s: document.values()) {
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


    public Asset getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(Asset previousVersion) {
        this.previousVersion = previousVersion;
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


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", file.getAbsolutePath())
                .toString();
    }
}
