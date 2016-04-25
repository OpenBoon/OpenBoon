package com.zorroa.archivist.sdk.domain;

import com.zorroa.archivist.sdk.schema.SourceSchema;

public class Asset extends Document implements EventLoggable {

    private String id;
    private long version;

    public Asset() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public SourceSchema getSource() {
        return getAttr("source", SourceSchema.class);
    }

    @Override
    public Object getLogId() {
        return id;
    }
}
