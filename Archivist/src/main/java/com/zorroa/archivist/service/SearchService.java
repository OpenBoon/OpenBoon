package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetAggregateBuilder;
import com.zorroa.archivist.sdk.domain.AssetSearchBuilder;
import com.zorroa.archivist.sdk.domain.AssetSuggestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearchBuilder builder);
    CountResponse count(AssetSearchBuilder builder);
    SuggestResponse suggest(AssetSuggestBuilder builder);
    SearchResponse aggregate(AssetAggregateBuilder builder);
    Iterable<Asset> scanAndScroll(AssetSearchBuilder builder);

}
