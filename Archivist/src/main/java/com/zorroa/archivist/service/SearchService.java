package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetSearchBuilder;
import org.elasticsearch.action.search.SearchResponse;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearchBuilder builder);
    Iterable<Asset> scanAndScroll(AssetSearchBuilder builder);

}
