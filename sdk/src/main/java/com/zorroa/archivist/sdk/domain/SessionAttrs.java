package com.zorroa.archivist.sdk.domain;

import java.util.Set;

/**
 * Created by chambers on 3/7/16.
 */
public class SessionAttrs {

    private AssetSearch search;
    private Set<String> selection;

    public Set<String> getSelection() {
        return selection;
    }

    public SessionAttrs setSelection(Set<String> selection) {
        this.selection = selection;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public SessionAttrs setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }
}
