package com.zorroa.archivist.domain;

public class ExportFileSpec {

    private String name;
    private String mimeType;
    private long size;

    public String getName() {
        return name;
    }

    public ExportFileSpec setName(String name) {
        this.name = name;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ExportFileSpec setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public long getSize() {
        return size;
    }

    public ExportFileSpec setSize(int size) {
        this.size = size;
        return this;
    }
}
