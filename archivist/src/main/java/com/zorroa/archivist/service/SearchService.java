package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.common.domain.PagedList;
import com.zorroa.common.domain.Paging;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.search.AssetAggregateBuilder;
import com.zorroa.sdk.search.AssetSearch;
import com.zorroa.sdk.search.AssetSuggestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;

import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearch builder);
    long count(AssetSearch builder);

    long count(Folder folder);

    SuggestResponse suggest(AssetSuggestBuilder builder);
    SearchResponse aggregate(AssetAggregateBuilder builder);
    Iterable<Asset> scanAndScroll(AssetSearch search);
    PagedList<Asset> getAll(Paging page, AssetSearch search);
    Map<String, Set<String>> getFields();
}
