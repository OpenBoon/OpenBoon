package com.zorroa.archivist.sdk.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

/**
 * Created by chambers on 11/23/15.
 */
public class Source implements Schema {

    private String path;
    private String extension;
    private String filename;
    private String basename;
    private String directory;

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

    @JsonIgnore
    @Override
    public String getNamespace() {
        return "source";
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(Source.class)
                .add("path", path)
                .add("ext", extension)
                .add("filename", filename)
                .add("basename", basename)
                .add("dirname", directory).toString();
    }
}
