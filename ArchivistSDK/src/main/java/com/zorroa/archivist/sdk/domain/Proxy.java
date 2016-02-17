package com.zorroa.archivist.sdk.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;

import java.awt.image.BufferedImage;

public class Proxy {
    private String name;
    private String path;
    private String uri;
    private int width;
    private int height;
    private String format;
    private BufferedImage image;

    public Proxy() { }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public Proxy setPath(String path) {
        this.path = path;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getName() {
        return name;
    }

    public Proxy setName(String name) {
        this.name = name;
        return this;
    }

    @JsonIgnore
    public BufferedImage getImage() {
        return image;
    }

    public Proxy setImage(BufferedImage image) {
        this.image = image;
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
