/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.sdk.domain;

/**
 * Sugest completions for text within a search
 */
public class AssetSuggestBuilder {
    String text;
    AssetSearch search;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public void setSearch(AssetSearch search) {
        this.search = search;
    }
}
