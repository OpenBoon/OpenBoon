package com.zorroa.archivist.sdk.domain;

import com.google.common.base.MoreObjects;

public class Proxy {

    private String name;
    private String uri;
    private int width;
    private int height;
    private String format;

    public Proxy() { }

    public String getUri() {
        return uri;
    }

    public Proxy setUri(String uri) {
        this.uri = uri;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public Proxy setWidth(int width) {
        this.width = width;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public Proxy setHeight(int height) {
        this.height = height;
        return this;
    }

    public String getFormat() {
        return format;
    }

    public Proxy setFormat(String format) {
        this.format = format;
        return this;
    }

    public String getName() {
        return name;
    }

    public Proxy setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proxy)) return false;

        Proxy proxy = (Proxy) o;

        if (getWidth() != proxy.getWidth()) return false;
        if (getHeight() != proxy.getHeight()) return false;
        return !(getUri() != null ? !getUri().equals(proxy.getUri()) : proxy.getUri() != null);

    }

    @Override
    public int hashCode() {
        int result = getUri() != null ? getUri().hashCode() : 0;
        result = 31 * result + getWidth();
        result = 31 * result + getHeight();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uri", uri)
                .add("name", name)
                .add("width", width)
                .add("height", height)
                .add("format", format)
                .toString();
    }
}
