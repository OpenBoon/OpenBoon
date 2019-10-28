package com.zorroa.archivist.schema;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.awt.geom.Point2D;

/**
 * LocationSchema defines attributes associated with geographical location.
 */
public class LocationSchema {

    /**
     * The longitude and latitude. Please note that geo-points
     * are ordered as: [lon,lat].  This is to conform to the format
     * used by GeoJSON.
     */
    private double[] point;

    /**
     * The altitude of the location.
     */
    private Integer altitude;

    /**
     * The time the location was obtained.
     */
    private Long time;

    /**
     * The nearest town or city.
     */
    private String city;

    /**
     * The country of the location.
     */
    private String country;

    public LocationSchema() { }

    public LocationSchema(Point2D point) {
        this.point = new double[] {
            point.getX(),
            point.getY()
        };
    }

    public LocationSchema(double[] point) {
        this.point = point;
    }

    public double[] getPoint() {
        return point;
    }

    @JsonIgnore
    public Point2D.Double getPoint2D() {
        return new Point2D.Double(point[0], point[1]);
    }

    @JsonIgnore
    public double getLatitude() {
        return point[1];
    }

    @JsonIgnore
    public double getLongitude() {
        return point[0];
    }

    public LocationSchema setPoint(double[] point) {
        this.point = point;
        return this;
    }

    public Integer getAltitude() {
        return altitude;
    }

    public LocationSchema setAltitude(Integer altitude) {
        this.altitude = altitude;
        return this;
    }

    public Long getTime() {
        return time;
    }

    public LocationSchema setTime(Long time) {
        this.time = time;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public LocationSchema setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCity() {
        return city;
    }

    public LocationSchema setCity(String city) {
        this.city = city;
        return this;
    }
}
