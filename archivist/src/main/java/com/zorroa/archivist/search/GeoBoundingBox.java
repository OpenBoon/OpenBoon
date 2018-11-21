package com.zorroa.archivist.search;

public class GeoBoundingBox {

    private String top_left;
    private String bottom_right;

    public GeoBoundingBox() { }

    public GeoBoundingBox(String tl, String br) {
        this.top_left = tl;
        this.bottom_right = br;
    }

    public String getTop_left() {
        return top_left;
    }

    public void setTop_left(String top_left) {
        this.top_left = top_left;
    }

    public String getBottom_right() {
        return bottom_right;
    }

    public void setBottom_right(String bottom_right) {
        this.bottom_right = bottom_right;
    }

    public double[] topLeftPoint() {
        String[] e = top_left.split(",\\s*", 2);
        return new double[] { Double.valueOf(e[0]), Double.valueOf(e[1]) };
    }

    public double[] bottomRightPoint() {
        String[] e = bottom_right.split(",\\s*", 2);
        return new double[] { Double.valueOf(e[0]), Double.valueOf(e[1]) };
    }
}
