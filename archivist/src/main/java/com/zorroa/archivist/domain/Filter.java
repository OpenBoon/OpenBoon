package com.zorroa.archivist.domain;

/**
 * Created by chambers on 8/9/16.
 */
public class Filter {

    private int id;
    private boolean enabled;
    private boolean matchAll;
    private String description;

    public int getId() {
        return id;
    }

    public Filter setId(int id) {
        this.id = id;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Filter setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Filter setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    public Filter setMatchAll(boolean matchAll) {
        this.matchAll = matchAll;
        return this;
    }
}
