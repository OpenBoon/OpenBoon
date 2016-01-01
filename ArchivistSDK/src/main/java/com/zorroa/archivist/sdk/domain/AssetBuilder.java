package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.archivist.sdk.schema.Keyword;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.schema.PermissionSchema;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;

public class AssetBuilder extends Document{

    private static final Logger logger = LoggerFactory.getLogger(AssetBuilder.class);

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

    public void setPreviousVersion(Asset asset) {
        if (asset == null) {
            return;
        }
        document.putAll(asset.getDocument());
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
