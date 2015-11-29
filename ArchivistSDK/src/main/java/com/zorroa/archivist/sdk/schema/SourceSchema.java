package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.zorroa.archivist.sdk.domain.AssetType;
import com.zorroa.archivist.sdk.util.IngestUtils;

import java.util.Date;

/**
 * Created by chambers on 11/23/15.
 */
public class SourceSchema implements Schema {

    @Keyword
    private String path;
    private String extension;
    private String filename;
    private String basename;
    private String directory;
    private Date date;
    private AssetType type;
    private String checksum;

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
        this.type = IngestUtils.determineAssetType(ext);
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

    public AssetType getType() {
        return type;
    }

    public void setType(AssetType type) {
        this.type = type;
    }

    @JsonIgnore
    @Override
    public String getNamespace() {
        return "source";
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(SourceSchema.class)
                .add("type", type)
                .add("path", path)
                .add("ext", extension)
                .add("filename", filename)
                .add("basename", basename)
                .add("dirname", directory).toString();
    }
}
