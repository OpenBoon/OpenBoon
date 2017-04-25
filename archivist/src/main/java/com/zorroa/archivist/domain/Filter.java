package com.zorroa.archivist.domain;

import com.zorroa.sdk.search.AssetSearch;

import java.util.Objects;

/**
 * Created by chambers on 8/9/16.
 */
public class Filter {

    private int id;
    private boolean enabled;
    private String description;
    private AssetSearch search;
    private Acl acl;

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

    public AssetSearch getSearch() {
        return search;
    }

    public Filter setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public Filter setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }

    public String getName() {
        return "filter_" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Filter filter = (Filter) o;
        return getId() == filter.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
