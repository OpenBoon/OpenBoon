package com.zorroa.common.config;

import com.google.common.base.MoreObjects;

import java.net.URI;

/**
 * Created by chambers on 4/11/17.
 */
public class NetworkEnvironment {

    public static final String ON_PREM = "on-prem";

    private URI uri;
    private String location;
    private String app;
    private String clusterAddr;

    public NetworkEnvironment() {}

    public String getApp() {
        return app;
    }

    public NetworkEnvironment setApp(String app) {
        this.app = app;
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public NetworkEnvironment setUri(URI uri) {
        this.uri = uri;
        return this;
    }

    public String getHostname() {
        return uri.getHost();
    }

    public int getPort() {
        return uri.getPort();
    }

    public String getLocation() {
        return location;
    }

    public NetworkEnvironment setLocation(String location) {
        this.location = location;
        return this;
    }

    public boolean isCloud() {
        return !location.equals(ON_PREM);
    }

    public String getClusterAddr() {
        return clusterAddr;
    }

    public NetworkEnvironment setClusterAddr(String clusterAddr) {
        this.clusterAddr = clusterAddr;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("app", app)
                .add("uri", uri)
                .add("location", location)
                .toString();
    }


}
