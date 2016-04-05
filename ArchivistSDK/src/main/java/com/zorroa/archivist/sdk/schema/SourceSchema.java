package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.zorroa.archivist.sdk.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Date;
import java.util.Set;

/**
 * The source schema contains everything that can be cleaned from the file path
 * itself without opening, reading or performing an otherwise potentially I/O blocking
 * operation.  The current exception to this rule in checksum which is being moved
 * to its own area.
 *
 * Every asset should at least have a valid source schema.
 *
 */
public class SourceSchema extends ExtendableSchema<String, Object> {

    private String type;
    private String path;
    private String extension;
    private String filename;
    private String basename;
    private String directory;
    private Date date = new Date();
    private String checksum;
    private Set<SourceSchema> representations;

    /**
     * File stat() values.
     */
    private Long fileSize;
    private Date timeCreated;
    private Date timeModified;
    private Date timeAccessed;
    private String owner;
    private String group;

    /**
     * The remote source URI represents where the original asset was obtained from.
     */
    private String remoteSourceUri;

    /**
     * The local source URI is where the asset can be streamed from.
     */
    private String objectStorageHost;

    public SourceSchema() {

    }

    public SourceSchema(File file) {
        setPath(file.getPath());
        setBasename(FileUtils.basename(getPath()));
        setDirectory(FileUtils.dirname(getPath()));
        setExtension(FileUtils.extension(getPath()));
        setFilename(FileUtils.filename(getPath()));

        try {
            PosixFileAttributes attrs = Files.getFileAttributeView(
                    file.toPath(), PosixFileAttributeView.class).readAttributes();

            setFileSize(attrs.size());
            setTimeAccessed(new Date(attrs.lastAccessTime().toMillis()));
            setTimeModified(new Date(attrs.lastModifiedTime().toMillis()));
            setTimeCreated(new Date(attrs.creationTime().toMillis()));
            setOwner(attrs.owner().getName());
            setGroup(attrs.group().getName());
        } catch (IOException e) {
            logger.warn("Failed to stat file '{}'", getPath(), e);
        }
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String ext) {
        this.extension = ext;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getBasename() {
        return basename;
    }

    public void setBasename(String basename) {
        this.basename = basename;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getType() {
        return type;
    }

    public SourceSchema setType(String type) {
        this.type = type;
        return this;
    }

    @JsonIgnore
    public boolean isSuperType(String type) {
        if (type == null) {
            return false;
        }
        return this.type.startsWith(type + "/");
    }

    @JsonIgnore
    public boolean isSubType(String type) {
        if (type == null) {
            return false;
        }
        return this.type.endsWith("/" + type);
    }

    @JsonIgnore
    public boolean isType(String type) {
        if (type == null) {
            return false;
        }
        return this.type.equals(type);
    }

    public Date getTimeCreated() {
        return timeCreated;
    }

    public SourceSchema setTimeCreated(Date timeCreated) {
        this.timeCreated = timeCreated;
        return this;
    }

    public Date getTimeModified() {
        return timeModified;
    }

    public SourceSchema setTimeModified(Date timeModified) {
        this.timeModified = timeModified;
        return this;
    }

    public Date getTimeAccessed() {
        return timeAccessed;
    }

    public SourceSchema setTimeAccessed(Date timeAccessed) {
        this.timeAccessed = timeAccessed;
        return this;
    }

    public String getOwner() {
        return owner;
    }

    public SourceSchema setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public SourceSchema setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getRemoteSourceUri() {
        return remoteSourceUri;
    }

    public SourceSchema setRemoteSourceUri(String remoteSourceUri) {
        this.remoteSourceUri = remoteSourceUri;
        return this;
    }

    public String getObjectStorageHost() {
        return objectStorageHost;
    }

    public SourceSchema setObjectStorageHost(String objectStorageHost) {
        this.objectStorageHost = objectStorageHost;
        return this;
    }

    public Set<SourceSchema> getRepresentations() {
        return representations;
    }

    public SourceSchema setRepresentations(Set<SourceSchema> representations) {
        this.representations = representations;
        return this;
    }

    public SourceSchema addRepresentation(SourceSchema source) {
        if (this.representations == null) {
            this.representations = Sets.newHashSet();
        }
        this.representations.add(source);
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SourceSchema.class)
                .add("path", path)
                .add("type", type)
                .add("ext", extension)
                .add("filename", filename)
                .add("basename", basename)
                .add("dirname", directory).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceSchema that = (SourceSchema) o;
        return Objects.equal(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPath());
    }
}
