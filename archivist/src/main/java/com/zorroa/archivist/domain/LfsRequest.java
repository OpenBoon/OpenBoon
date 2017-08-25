package com.zorroa.archivist.domain;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class LfsRequest {

    private String path;
    private String prefix;
    private Set<String> types = ImmutableSet.of();

    public String getPath() {
        return path;
    }

    public LfsRequest setPath(String path) {
        this.path = path;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public LfsRequest setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public Set<String> getTypes() {
        return types;
    }

    public LfsRequest setTypes(Set<String> types) {
        this.types = types;
        return this;
    }
}
