package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;
import com.zorroa.archivist.sdk.schema.Keyword;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.schema.PermissionSchema;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;

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

    /**
     * An fileinput stream that can be reset.  Much of the Java image manipulation or
     * metadata APIs take an InputStream as an argument, but in general you can't go
     * backwards with InputStreams.
     *
     * Calling getInputStream() the first time will create a new input stream.
     * Subsequent calls to getInputStream() will return the same input stream
     * reset to position 0.
     *
     */
    private MarkableFileInputStream inputStream;

    /**
     * The image representation of this asset.  An asset, may not be an image, it might
     * be a PDF or a word document, but to make a proxy for this asset we need an image
     * of some kind or not proxy can be made.
     *
     */
    private BufferedImage image;

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

        try {
            source.setFileSize(Files.size(file.toPath()));
        } catch (IOException e) {
            source.setFileSize(0);
        }

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

    public AssetBuilder addKeywords(double confidence, boolean suggest, Iterable<String> words) {
        keywords.addKeywords(confidence, suggest, words);
        return this;
    }

    public SourceSchema getSource() {
        return source;
    }

    public KeywordsSchema getKeywords() {
        return keywords;
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
                    if (field.get(s) == null) {
                        continue;
                    }
                    keywords.addKeywords(annotation.confidence(), false, field.get(s).toString());
                } catch (Exception e) {
                    logger.warn("Failed to access field '{}' on class '{}' marked with @Keyword, on asset {} ",
                            field.getName(), s.getClass().getCanonicalName(), source.getPath());
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

    public BufferedImage getImage() {
        return this.image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public InputStream getInputStream() {
        if (inputStream == null) {
            try {
                inputStream = new MarkableFileInputStream(new FileInputStream(file));
            } catch (FileNotFoundException e) {
                // shouldn't really happen but throw a runtime if it does.
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                inputStream.reset();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return inputStream;
    }

    public boolean isSuperType(String type) {
        if (type == null) {
            return false;
        }
        return this.source.getType().startsWith(type + "/");
    }

    public boolean isSubType(String type) {
        if (type == null) {
            return false;
        }
        return this.source.getType().endsWith("/" + type);
    }

    public boolean isType(String type) {
        if (type == null) {
            return false;
        }
        return this.source.isType(type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", file.getAbsolutePath())
                .toString();
    }
}
