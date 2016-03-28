package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetAggregateBuilder;
import com.zorroa.archivist.sdk.domain.AssetSearch;
import com.zorroa.archivist.sdk.domain.AssetSuggestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;

import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearch builder);
    CountResponse count(AssetSearch builder);
    SuggestResponse suggest(AssetSuggestBuilder builder);
    SearchResponse aggregate(AssetAggregateBuilder builder);
    Iterable<Asset> scanAndScroll(AssetSearch search);

    /**
     * Return the total file size for the given search.
     *
     * @param builder
     * @return
     */
    long getTotalFileSize(AssetSearch builder);

    Map<String, Set<String>> getFields();
}
