package com.zorroa.archivist.service;

import com.zorroa.archivist.domain.Folder;
import com.zorroa.sdk.domain.Asset;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.AssetSearch;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by chambers on 9/25/15.
 */
public interface SearchService {
    SearchResponse search(AssetSearch builder);
    long count(AssetSearch builder);

    List<Long> count(List<Integer> ids, AssetSearch search);

    long count(Folder folder);

    SuggestResponse suggest(String text);
    List<String> getSuggestTerms(String text);

    Iterable<Asset> scanAndScroll(AssetSearch search, int maxResults);

    /**
     * Execute the AssetSearch with the given Paging object.
     *
     * @param page
     * @param search
     * @return
     */
    PagedList<Asset> search(Pager page, AssetSearch search);

    void search(Pager page, AssetSearch search, OutputStream stream) throws IOException;

    /**
     * Return the next page of an asset scroll.
     *
     * @param id
     * @param timeout
     * @return
     */
    PagedList<Asset> scroll(String id, String timeout);

    SearchRequestBuilder buildSearch(AssetSearch search);

    QueryBuilder getQuery(AssetSearch search);

    Map<String, Set<String>> getFields();

    Map<String, Float> getQueryFields();

    List<String> analyzeQuery(AssetSearch search);
}
