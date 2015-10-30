package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.AssetSearchBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearchBuilder builder);
}
