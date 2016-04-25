package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * An annotation represents multiple shapes, all drawn with the same pen.
 **/
public class Annotation {

    private final String type = "geometrycollection";
    private List<Shape> geometries;
    private Pen pen = new Pen(3, "#FFFFFF");

    public Annotation() { }

    public Annotation(Pen pen) {
        this.pen = pen;
        this.geometries = Lists.newArrayList();
    }

    public String getType() {
        return type;
    }

    public List<Shape> getGeometries() {
        return geometries;
    }

    public Annotation setGeometries(List<Shape> geometries) {
        this.geometries = geometries;
        return this;
    }

    public Annotation addToGeometries(Shape shape) {
        if (this.geometries == null) {
            this.geometries = Lists.newArrayList();
        }
        this.geometries.add(shape);
        return this;
    }

    public Pen getPen() {
        return pen;
    }

    public Annotation setPen(Pen pen) {
        this.pen = pen;
        return this;
    }
}
