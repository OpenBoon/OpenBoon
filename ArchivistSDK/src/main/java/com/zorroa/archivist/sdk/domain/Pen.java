package com.zorroa.archivist.sdk.domain;

/**
 * TODO: regex on color
 * TODO: check pen width > 0
 */
public class Pen {

    private int width;
    private String color;
    boolean filled = false;

    public Pen() { }

    public Pen(int width) {
        this.width = width;
        this.color = "#FFFFFF";
    }

    public Pen(int width, String color) {
        this.width = width;
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public Pen setWidth(int width) {
        this.width = width;
        return this;
    }

    public String getColor() {
        return color;
    }

    public Pen setColor(String color) {
        this.color = color;
        return this;
    }
}
