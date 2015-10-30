package com.zorroa.archivist.sdk.domain;

import java.io.Serializable;

public class ProxyOutput implements Serializable {

    private static final long serialVersionUID = 7766478828301345429L;

    private int size;
    private int bpp;
    private String format;

    public ProxyOutput() { }

    public ProxyOutput(String format, int size, int bpp) {
        this.format = format;
        this.size = size;
        this.bpp = bpp;
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

    public String toString() {
        return String.format("<ProxyOutput(\"%s\",%d,%d)>", format, size, bpp);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ProxyOutput)) {
            return false;
        }

        return obj.toString().equals(this.toString());
    }
}
