package com.zorroa.archivist.sdk.domain;

public class Proxy {
    private String path;
    private int width;
    private int height;
    private String format;

    public Proxy() { }

    public String getPath() {
        return path;
    }

    public void setPathyeh (String path) {
        this.path = path;
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
}
