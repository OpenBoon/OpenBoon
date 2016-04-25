package com.zorroa.archivist.sdk.domain;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by chambers on 4/8/16.
 */
public class Shape {

    private String type;

    /**
     * This field can be a few different types, an array of floats, an array of
     * array of floats, or array of array of array of floats.  For this reason
     * we've left it as a generic List<Object>.
     */
    private List<Object> coordinates;

    public static final Shape newPoint(double x, double y) {
        return new Shape("point", ImmutableList.of(x, y));
    }

    public Shape() { }

    public Shape(String type) {
        this.type = type;
    }

    public Shape(String type, List<Object> coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }

    public String getType() {
        return type;
    }

    public Shape setType(String type) {
        this.type = type;
        return this;
    }

    public List<Object> getCoordinates() {
        return coordinates;
    }

    public Shape setCoordinates(List<Object> coordinates) {
        this.coordinates = coordinates;
        return this;
    }
}
