/*
 * Copyright (c) 2015 by Zorroa
 */

package com.zorroa.archivist.search;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "Asset Suggest Builder", description = "Suggest completions for text within a search")
public class AssetSuggestBuilder {

    @ApiModelProperty("Text to suggest auto-completion for.")
    String text;

    @ApiModelProperty("Suggestion will be gathered only from Assets that match this search filter.")
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
