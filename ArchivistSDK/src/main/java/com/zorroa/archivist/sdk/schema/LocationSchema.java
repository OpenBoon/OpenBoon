package com.zorroa.archivist.sdk.schema;

import com.zorroa.archivist.sdk.domain.Shape;

/**
 * Created by chambers on 4/11/16.
 */
public class LocationSchema {

    private Shape location;
    private Integer altitude;
    private Long time;
    private String city;
    private String country;

    public LocationSchema() { }

    public LocationSchema(Shape location) {
        this.location = location;
    }

    public Shape getLocation() {
        return location;
    }

    public LocationSchema setLocation(Shape location) {
        this.location = location;
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
