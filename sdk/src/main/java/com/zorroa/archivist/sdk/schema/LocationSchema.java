package com.zorroa.archivist.sdk.schema;

/**
 * Created by chambers on 4/11/16.
 */
public class LocationSchema {

    private double[] point;
    private Integer altitude;
    private Long time;
    private String city;
    private String country;

    public LocationSchema() { }

    public LocationSchema(double[] point) {
        this.point = point;
    }

    public double[] getPoint() {
        return point;
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
