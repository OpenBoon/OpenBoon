package com.zorroa.archivist.domain;

import java.net.URI;

/**
 * Created by chambers on 4/11/17.
 */
public class NetworkEnvironment {

    public static final String ON_PREM = "on-prem";

    private URI uri;
    private String location;

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

    public String toString() {
        return "<Archivist: " + uri.toString() + ">";
    }
}
