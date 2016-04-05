package com.zorroa.archivist.sdk.domain;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import com.google.common.base.MoreObjects;
import com.zorroa.archivist.sdk.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.UUID;

public class AssetBuilder extends Document {

    private static final Logger logger = LoggerFactory.getLogger(AssetBuilder.class);

    private static final NameBasedGenerator uuidGenerator = Generators.nameBasedGenerator();

    private final UUID id;

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
     * Describes relationships between assets.
     */
    private LinkSchema links;

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

    /**
     * Return true of the asset already exists and this builder is going to update, not
     * create the asset;
     */
    private boolean update = false;

    /**
     * The 'changed' property is true when an asset is new or the file size or modified
     * date has changed.
     */
    private boolean changed = true;


    public AssetBuilder(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException(
                "AssetBuilder must point to a regular file.");
        }

        this.file = file;
        this.id = uuidGenerator.generate(file.getAbsolutePath());

        /*
         * Add the standard schemas.
         */

        source = new SourceSchema(file);
        setAttr("source", source);
        setAttr("user", new UserSchema());

        keywords = new KeywordsSchema();
        setAttr("keywords", keywords);

        permissions = new PermissionSchema();
        setAttr("permissions", permissions);

        links = new LinkSchema();
        setAttr("links", links);
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

    public LinkSchema getLinks() {
        return links;
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

    public boolean isUpdate() {
        return update;
    }

    public void setPreviousVersion(Asset asset) {
        if (asset == null) {
            return;
        }

        SourceSchema currentSource = asset.getAttr("source", SourceSchema.class);
        if (currentSource.getTimeModified() == source.getTimeModified() ||
                currentSource.getFileSize() == source.getFileSize()) {
            setChanged(false);
        }

        update = true;
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

    public void setWritePermissions(Collection<Permission> perms) {
        permissions.getWrite().clear();
        for (Permission p: perms) {
            permissions.getWrite().add(p.getId());
        }
    }

    public void setSearchPermissions(Collection<Permission> perms) {
        permissions.getSearch().clear();
        for (Permission p: perms) {
            permissions.getSearch().add(p.getId());
        }
    }

    public void setExportPermissions(Collection<Permission> perms) {
        permissions.getExport().clear();
        for (Permission p: perms) {
            permissions.getExport().add(p.getId());
        }
    }

    public String getBasename() {
        String path = file.getName();
        try {
            return path.substring(0, path.lastIndexOf('.'));
        } catch (IndexOutOfBoundsException ignore) { /*EMPTY*/ }
        return "";
    }

    public String getExtension() {
        String path = file.getName();
        try {
            return path.substring(path.lastIndexOf('.') + 1);
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

    public boolean isChanged() {
        return changed;
    }

    public AssetBuilder setChanged(boolean changed) {
        this.changed = changed;
        return this;
    }


    public UUID getId() {
        return id;
    }

    public void close() {
        if (inputStream != null) {
            try {
                inputStream.close();
            }
            catch (Exception ignore) {

            }
            inputStream = null;
        }

        this.image = null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", file.getAbsolutePath())
                .toString();
    }
}
