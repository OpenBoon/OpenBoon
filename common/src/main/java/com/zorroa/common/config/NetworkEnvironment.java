package com.zorroa.common.config;

import com.google.common.base.MoreObjects;

import java.net.URI;

/**
 * Created by chambers on 4/11/17.
 */
public class NetworkEnvironment {

    public static final String ON_PREM = "on-prem";

    /**
     * The REST API interface used by external clients.
     */
    private URI publicUri;

    /**
     * The REST API interface used by internal clients. (might be the same)
     */
    private URI privateUri;

    /**
     * The communication port.
     */
    private int clusterPort;

    private String location;
    private String app;

    public NetworkEnvironment() {}

    /**
     * The cluster communication address (host:port) which is a
     * combination of the private URI host and cluster port.
     *
     * @return
     */
    public String getClusterAddr() {
        return privateUri.getHost() + ":" + clusterPort;
    }

    public String getApp() {
        return app;
    }

    public NetworkEnvironment setApp(String app) {
        this.app = app;
        return this;
    }

    public URI getPublicUri() {
        return publicUri;
    }

    public NetworkEnvironment setPublicUri(URI publicUri) {
        this.publicUri = publicUri;
        return this;
    }

    public URI getPrivateUri() {
        return privateUri;
    }

    public NetworkEnvironment setPrivateUri(URI privateUri) {
        this.privateUri = privateUri;
        return this;
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

    public int getClusterPort() {
        return clusterPort;
    }

    public NetworkEnvironment setClusterPort(int clusterPort) {
        this.clusterPort = clusterPort;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("app", app)
                .add("uri", publicUri)
                .add("location", location)
                .toString();
    }


}
