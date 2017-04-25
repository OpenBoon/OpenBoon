package com.zorroa.archivist.domain;

import com.zorroa.sdk.search.AssetSearch;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by chambers on 8/9/16.
 */
public class FilterSpec {

    @NotEmpty
    private String description;

    private boolean enabled = true;

    @NotEmpty
    private AssetSearch search;

    @NotEmpty
    private Acl acl;

    public String getDescription() {
        return description;
    }

    public FilterSpec setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public FilterSpec setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public FilterSpec setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }

    public Acl getAcl() {
        return acl;
    }

    public FilterSpec setAcl(Acl acl) {
        this.acl = acl;
        return this;
    }
}
