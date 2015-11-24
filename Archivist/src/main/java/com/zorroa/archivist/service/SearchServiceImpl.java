package com.zorroa.archivist.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator;
import com.zorroa.archivist.repository.FolderDao;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.sdk.domain.*;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    FolderDao folderDao;

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Override
    public SearchResponse search(AssetSearchBuilder builder) {
        return buildSearch(builder).get();
    }

    @Override
    public CountResponse count(AssetSearchBuilder builder) {
        return buildCount(builder).get();
    }

    @Override
    public SuggestResponse suggest(AssetSuggestBuilder builder) {
        return buildSuggest(builder).get();
    }

    @Override
    public SearchResponse aggregate(AssetAggregateBuilder builder) {
        return buildAggregate(builder).get();
    }

    public Iterable<Asset> scanAndScroll(AssetSearchBuilder builder) {

        SearchResponse rsp = client.prepareSearch(alias)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(getQuery(builder))
                .setSize(100).execute().actionGet();

        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private SearchRequestBuilder buildSearch(AssetSearchBuilder builder) {

        SearchRequestBuilder search = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(builder));
        logger.info(search.toString());

        /*
         * alternative sorting and paging here.
         */

        return search;
    }

    private CountRequestBuilder buildCount(AssetSearchBuilder builder) {
        CountRequestBuilder count = client.prepareCount(alias)
                .setTypes("asset")
                .setQuery(getQuery(builder));
        logger.info(count.toString());
        return count;
    }

    private SuggestRequestBuilder buildSuggest(AssetSuggestBuilder builder) {
        // FIXME: We need to use builder.search in here somehow!
        CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder("completions")
                .text(builder.getText())
                .field("keywords_suggest");
        SuggestRequestBuilder suggest = client.prepareSuggest(alias)
                .addSuggestion(completion);
        return  suggest;
    }

    private SearchRequestBuilder buildAggregate(AssetAggregateBuilder builder) {
        AssetSearchBuilder search = builder.getSearch();
        if (search == null) {
            search = new AssetSearchBuilder();      // Use default empty search == all
        }
        SearchRequestBuilder aggregation = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search))
                .setAggregations(builder.getAggregations())
                .setSearchType(SearchType.COUNT);
        return aggregation;
    }

    private QueryBuilder getQuery(AssetSearchBuilder builder) {
        QueryBuilder query;
        if (builder.getQuery() != null) {
            query = QueryBuilders.queryStringQuery(builder.getQuery())
                    .field("keywords.indexed")
                    .field("keywords.untouched", 2)
                    .lenient(true)
                    .fuzzyPrefixLength(3)
                    .analyzer("standard");
        } else {
            query = QueryBuilders.matchAllQuery();
        }

        return QueryBuilders.filteredQuery(query, getFilter(builder.getFilter()));
    }

    /**
     * Builds an "AND" filter based on all the options in the AssetSearchBuilder.
     *
     * @param builder
     * @return
     */
    private FilterBuilder getFilter(AssetFilter builder) {
        AndFilterBuilder filter = FilterBuilders.andFilter();

        filter.add(SecurityUtils.getPermissionsFilter());

        if (builder == null) {
            return filter;
        }

        if (builder.getCreatedAfterTime() != null || builder.getCreatedBeforeTime() != null) {
            RangeFilterBuilder createTimeFilter = FilterBuilders.rangeFilter("timeCreated");
            if (builder.getCreatedAfterTime() != null) {
                createTimeFilter.gte(builder.getCreatedAfterTime());
            }
            if (builder.getCreatedBeforeTime() != null) {
                createTimeFilter.lte(builder.getCreatedBeforeTime());
            }
            filter.add(createTimeFilter);
        }

        if (builder.getFolderIds() != null) {
            filter.add(getFolderFilter(builder));
        }

        if (builder.getExportId() > 0) {
            FilterBuilder exportFilterBuilder = FilterBuilders.termFilter("exports", builder.getExportId());
            filter.add(exportFilterBuilder);
        }

        if (builder.getExistFields() != null) {
            for (String term : builder.getExistFields()) {
                FilterBuilder existsFilterBuilder = FilterBuilders.existsFilter(term);
                filter.add(existsFilterBuilder);
            }
        }

        if (builder.getFieldTerms() != null) {
            for (AssetFieldTerms fieldTerms : builder.getFieldTerms()) {
                FilterBuilder termsFilterBuilder = FilterBuilders.termsFilter(fieldTerms.getField(), fieldTerms.getTerms());
                filter.add(termsFilterBuilder);
            }
        }

        if (builder.getFieldRanges() != null) {
            for (AssetFieldRange fieldRange : builder.getFieldRanges()) {
                FilterBuilder rangeFilterBuilder = FilterBuilders.rangeFilter(fieldRange.getField())
                        .gte(fieldRange.getMin())
                        .lt(fieldRange.getMax());
                filter.add(rangeFilterBuilder);
            }
        }

        if (builder.getScripts() != null) {
            for (AssetScript script : builder.getScripts()) {
                FilterBuilder scriptFilterBuilder = FilterBuilders.scriptFilter(script.getName())
                        .lang("native")
                        .params(script.getParams());
                filter.add(scriptFilterBuilder);
            }
        }

        return filter;
    }

    private final LoadingCache<String, Set<String>> childCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(1, TimeUnit.DAYS)
        .build(
            new CacheLoader<String, Set<String>>() {
                public Set<String> load(String key) throws Exception {
                    Set<String> result =  Collections.synchronizedSet(folderDao.getChildren(key).stream().map(
                            Folder::getId).collect(Collectors.toSet()));
                    return result;
                }
            });

    public FilterBuilder getFolderFilter(AssetFilter builder) {
        Set<String> result = Sets.newHashSetWithExpectedSize(100);
        Queue<String> queue = Queues.newLinkedBlockingQueue();

        result.addAll(builder.getFolderIds());
        queue.addAll(builder.getFolderIds());
        getChildrenRecursive(result, queue);

        return FilterBuilders.termsFilter("folders", result);
    }

    private void getChildrenRecursive(Set<String> result, Queue<String> toQuery) {

        while(true) {
            String current = toQuery.poll();
            if (current == null) {
                return;
            }
            if (Folder.ROOT_ID.equals(current)) {
                continue;
            }
            try {
                Set<String> children = childCache.get(current);
                if (children.isEmpty()) {
                    continue;
                }
                toQuery.addAll(children);
                result.addAll(children);

            } catch (Exception e) {
                logger.warn("Failed to obtain child folders for {}", current, e);
            }
        }
    }
}
