package com.zorroa.archivist.sdk.domain;

public class Asset extends Document {

    private String id;
    private long version;

    public Asset() { }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AssetType getType() {
        int type = getAttr("source.type");
        return AssetType.values()[type];
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }


}
