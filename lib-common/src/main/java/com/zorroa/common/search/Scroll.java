package com.zorroa.common.search;

/**
 * Created by chambers on 9/29/16.
 */
public class Scroll {

    private String timeout = "1m";
    private String id;

    public Scroll() { }

    public Scroll(String id) {
        this.id = id;
    }

    public String getTimeout() {
        return timeout;
    }

    public Scroll setTimeout(String timeout) {
        this.timeout = timeout;
        return this;
    }

    public String getId() {
        return id;
    }

    public Scroll setId(String id) {
        this.id = id;
        return this;
    }
}
