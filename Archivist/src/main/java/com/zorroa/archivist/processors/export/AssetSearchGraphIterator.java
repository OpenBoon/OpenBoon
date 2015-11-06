package com.zorroa.archivist.processors.export;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetSearchBuilder;
import com.zorroa.archivist.sdk.processor.export.*;
import com.zorroa.archivist.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * An ExportInput processor that takes an asset search and feeds
 * and export graph.
 */
public class AssetSearchGraphIterator implements AssetIterator {

    private static final Logger logger = LoggerFactory.getLogger(AssetSearchGraphIterator.class);

    @Autowired
    SearchService searchService;

    private AssetSearchBuilder search;

    public AssetSearchGraphIterator() {}

    public AssetSearchGraphIterator(AssetSearchBuilder search) {
        this.search = search;
    }

    public AssetSearchBuilder getSearch() {
        return search;
    }

    public void setSearch(AssetSearchBuilder search) {
        this.search = search;
    }

    @Override
    public Iterable<Asset> getIterator() {
        return searchService.scanAndScroll(search);
    }
}
