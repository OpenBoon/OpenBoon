package com.zorroa.archivist.sdk.domain;

/**
 * Created by chambers on 11/9/15.
 */
public class ConnectionBuilder {

    private String[] cord;
    private String[] socket;

    public ConnectionBuilder() { }

    public ConnectionBuilder(String srcPort, String dstPort) {
        this.cord = srcPort.split("::", 2);
        this.socket = dstPort.split("::", 2);
    }

    public String[] getCord() {
        return cord;
    }

    public ConnectionBuilder setCord(String[] cord) {
        this.cord = cord;
        return this;
    }

    public String[] getSocket() {
        return socket;
    }

    public ConnectionBuilder setSocket(String[] socket) {
        this.socket = socket;
        return this;
    }
}
