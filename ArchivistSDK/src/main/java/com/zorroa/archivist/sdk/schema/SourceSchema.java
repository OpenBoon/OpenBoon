package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.util.Date;
import java.util.List;

/**
 * The source schema contains everything that can be cleaned from the file path
 * itself without opening, reading or performing an otherwise potentially I/O blocking
 * operation.  The current exception to this rule in checksum which is being moved
 * to its own area.
 *
 * Every asset should at least have a valid source schema.
 *
 */
public class SourceSchema implements Schema {

    @Keyword
    private String path;
    private String extension;
    private String filename;
    private String basename;
    private String directory;
    private Date date = new Date();
    private String checksum;
    private List<String> relatedPaths;

    /**
     * File stat() values.
     */
    private long fileSize;
    private Date timeCreated;
    private Date timeModified;
    private Date timeAccessed;
    private String owner;
    private String group;

    @Keyword
    private String type;

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

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
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

    public void setRelatedPaths(List<String> relatedPaths) {
        this.relatedPaths = relatedPaths;
    }

    public List<String> getRelatedPaths() {
        return relatedPaths;
    }

    public void addToRelatedPaths(String path) {
        if (relatedPaths == null) {
            relatedPaths = Lists.newArrayList();
        }
        this.relatedPaths.add(path);
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

    @JsonIgnore
    @Override
    public String getNamespace() {
        return "source";
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
}
