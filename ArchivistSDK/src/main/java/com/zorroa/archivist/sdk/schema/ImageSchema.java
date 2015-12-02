package com.zorroa.archivist.sdk.schema;

import java.awt.geom.Point2D;

/**
 * Image Schem contains options that only pertain to Image assets.
 */
public class ImageSchema implements Schema {

    private Point2D.Double location;
    private Integer width;
    private Integer height;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Point2D.Double getLocation() {
        return location;
    }

    public void setLocation(Point2D.Double location) {
        this.location = location;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    @Override
    public String getNamespace() {
        return "image";
    }

}
