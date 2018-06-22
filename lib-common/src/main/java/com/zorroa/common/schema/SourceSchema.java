package com.zorroa.common.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zorroa.common.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.*;

public class SourceSchema {

    /**
     * The id key, combined with the file path, determines the ID of a file source.
     */
    private String idkey;

    /**
     * The media type (also refered to as MIME Type or content type) of the file.
     */
    private String mediaType;

    /**
     * The type of file, for example text, image, or video.
     */
    private String type;

    /**
     * The sub type of the file, for example html, mpeg, or jpeg.
     */
    private String subType;

    private String path;
    private String extension;
    private String filename;
    private String basename;
    private String directory;

    private Date timeCreated;
    private Date timeModified;

    private Set<String> keywords;

    private Boolean exists;

    /**
     * File stat() values.  Defaults to 0.
     */
    private Long fileSize;

    public SourceSchema() { }

    public SourceSchema(Path file) {
        this(file.toFile());
    }

    public SourceSchema(File file) {
        setPath(FileUtils.normalize(file).toString());
        setBasename(FileUtils.basename(getPath()));
        setDirectory(FileUtils.dirname(getPath()));
        setExtension(FileUtils.extension(getPath()));
        setFilename(FileUtils.filename(getPath()));
        setMediaType(FileUtils.getMediaType(getPath()));
        setFileSize(0L);
        setExists(Files.exists(file.toPath()));

        try {
            PosixFileAttributes attrs = Files.getFileAttributeView(
                    file.toPath(), PosixFileAttributeView.class).readAttributes();

            setFileSize(attrs.size());
            setTimeCreated(new Date(attrs.creationTime().toMillis()));
            setTimeModified(new Date(attrs.lastModifiedTime().toMillis()));
        } catch (IOException e) {
            /**
             * Sometimes the file might not be a valid file, but becomes
             * a valid file later on, so we just ignore this.
             */
        }
    }

    public void resetSourceFile(File file) {
        setPath(FileUtils.normalize(file).toString());
        setBasename(FileUtils.basename(getPath()));
        setDirectory(FileUtils.dirname(getPath()));
        setExtension(FileUtils.extension(getPath()));
        setFilename(FileUtils.filename(getPath()));
        setMediaType(FileUtils.getMediaType(getPath()));
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMediaType() {
        return mediaType;
    }

    public SourceSchema setMediaType(String mediaType) {
        this.mediaType = mediaType;
        String[] p = this.mediaType.split("/", 2);
        this.type = p[0];
        this.subType = p[1];
        return this;
    }

    public String getSubType() {
        return subType;
    }

    public SourceSchema setSubType(String subType) {
        this.subType = subType;
        return this;
    }

    public String getType() {
        return type;
    }

    public SourceSchema setType(String type) {
        this.type = type;
        return this;
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

    public String getIdkey() {
        return idkey;
    }

    public SourceSchema setIdkey(String idkey) {
        this.idkey = idkey;
        return this;
    }

    public Set<String> getKeywords() {
        return keywords;
    }

    public SourceSchema setKeywords(Set<String> keywords) {
        this.keywords = keywords;
        return this;
    }

    public void addToKeywords(Collection<String> words) {
        if (words == null) {
            return;
        }
        if (this.keywords == null) {
            this.keywords = new HashSet();
        }
        this.keywords.addAll(words);
    }

    public Boolean getExists() {
        return exists;
    }

    public SourceSchema setExists(Boolean exists) {
        this.exists = exists;
        return this;
    }

    @JsonIgnore
    public boolean isMediaType(String type) {
        return this.mediaType.endsWith(type);
    }

    @JsonIgnore
    public boolean isSubType(String type) {
        return this.subType.equals(type);
    }

    @JsonIgnore
    public boolean isType(String type) {
        return this.type.equals(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceSchema that = (SourceSchema) o;
        return Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getPath());
    }
}
