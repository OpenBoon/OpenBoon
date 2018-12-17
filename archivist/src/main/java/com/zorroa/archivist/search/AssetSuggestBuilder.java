/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

/**
 * Sugest completions for text within a search
 */
public class AssetSuggestBuilder {
    String text;
    AssetSearch search;

    public String getText() {
        return text;
    }

    public AssetSuggestBuilder setText(String text) {
        this.text = text;
        return this;
    }

    public AssetSearch getSearch() {
        return search;
    }

    public AssetSuggestBuilder setSearch(AssetSearch search) {
        this.search = search;
        return this;
    }
}
