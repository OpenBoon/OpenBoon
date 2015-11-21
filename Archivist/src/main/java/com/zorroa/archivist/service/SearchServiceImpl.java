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
import com.zorroa.archivist.sdk.domain.Asset;
import com.zorroa.archivist.sdk.domain.AssetSearchBuilder;
import com.zorroa.archivist.sdk.domain.Folder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.*;
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

        return QueryBuilders.filteredQuery(query, getFilter(builder));
    }

    /**
     * Builds an "AND" filter based on all the options in the AssetSearchBuilder.
     *
     * @param builder
     * @return
     */
    private FilterBuilder getFilter(AssetSearchBuilder builder) {
        AndFilterBuilder filter = FilterBuilders.andFilter();
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

        filter.add(SecurityUtils.getPermissionsFilter());

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

    public FilterBuilder getFolderFilter(AssetSearchBuilder builder) {
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
