package com.zorroa.archivist.domain;

import java.util.Objects;

public class Blob implements BlobId {

    private int blobId;
    private long version;

    private String app;
    private String feature;
    private String name;

    private Object data;
    private Acl acl;

    public String getApp() {
        return app;
    }

    public Blob setApp(String app) {
        this.app = app;
        return this;
    }

    public String getFeature() {
        return feature;
    }

    public Blob setFeature(String feature) {
        this.feature = feature;
        return this;
    }

    public String getName() {
        return name;
    }

    public Blob setName(String name) {
        this.name = name;
        return this;
    }

    public Object getData() {
        return data;
    }

    public Blob setData(Object data) {
        this.data = data;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public Blob setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public int getBlobId() {
        return blobId;
    }

    public Blob setBlobId(int blobId) {
        this.blobId = blobId;
        return this;
    }

    public long getVersion() {
        return version;
    }

    public Blob setVersion(long version) {
        this.version = version;
        return this;
    }

    public String getPath() {
        return String.join("/", getApp(), getFeature(), getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Blob blob = (Blob) o;
        return getBlobId() == blob.getBlobId();
    }

    @Override
    public int hashCode() {

        return Objects.hash(getBlobId());
    }
}
