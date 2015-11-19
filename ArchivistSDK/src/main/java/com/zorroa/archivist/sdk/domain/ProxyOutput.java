package com.zorroa.archivist.sdk.domain;

import java.io.Serializable;

public class ProxyOutput implements Serializable {

    private static final long serialVersionUID = 7766478828301345429L;

    private int size;
    private int bpp;
    private String format;
    private float quality;

    public ProxyOutput() { }

    public ProxyOutput(String format, int size, int bpp, float quality) {
        this.format = format;
        this.size = size;
        this.bpp = bpp;
        this.quality = quality;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getBpp() {
        return bpp;
    }

    public void setBpp(int bpp) {
        this.bpp = bpp;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public float getQuality() {
        return quality;
    }

    public void setQuality(float quality) {
        this.quality = quality;
    }

    public String toString() {
        return String.format("<ProxyOutput(\"%s\",%d,%d,%f)>", format, size, bpp, quality);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ProxyOutput)) {
            return false;
        }

        return obj.toString().equals(this.toString());
    }
}
