package com.zorroa.archivist.service;

import com.google.common.collect.Sets;
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
import com.zorroa.archivist.security.SecurityUtils;
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

import java.util.Set;

/**
 * Created by chambers on 9/25/15.
 */
@Service
public class SearchServiceImpl implements SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    @Autowired
    PermissionDao permissionDao;

    @Autowired
    RoomService roomService;

    @Autowired
    UserService userService;

    @Autowired
    FolderService folderService;

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

    public Iterable<Asset> scanAndScroll(AssetSearch search) {

        SearchResponse rsp = client.prepareSearch(alias)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(getQuery(search))
                .setSize(100).execute().actionGet();

        return new ScanAndScrollAssetIterator(client, rsp);
    }

    private SearchRequestBuilder buildSearch(AssetSearchBuilder builder) {

        SearchRequestBuilder search = client.prepareSearch(alias)
                .setTypes("asset")
                .setSize(builder.getSize())
                .setFrom(builder.getFrom())
                .setQuery(getQuery(builder.getSearch()));
        logger.info(search.toString());

        /*
         * alternative sorting and paging here.
         */

        return search;
    }

    private CountRequestBuilder buildCount(AssetSearchBuilder builder) {
        CountRequestBuilder count = client.prepareCount(alias)
                .setTypes("asset")
                .setQuery(getQuery(builder.getSearch()));
        logger.info(count.toString());
        return count;
    }

    private SuggestRequestBuilder buildSuggest(AssetSuggestBuilder builder) {
        // FIXME: We need to use builder.search in here somehow!
        CompletionSuggestionBuilder completion = new CompletionSuggestionBuilder("completions")
                .text(builder.getText())
                .field("keywords.suggest");
        SuggestRequestBuilder suggest = client.prepareSuggest(alias)
                .addSuggestion(completion);
        return  suggest;
    }

    private SearchRequestBuilder buildAggregate(AssetAggregateBuilder builder) {
        AssetSearch search = builder.getSearch();
        if (search == null) {
            search = new AssetSearch();      // Use default empty search == all
        }
        SearchRequestBuilder aggregation = client.prepareSearch(alias)
                .setTypes("asset")
                .setQuery(getQuery(search))
                .setAggregations(builder.getAggregations())
                .setSearchType(SearchType.COUNT);
        return aggregation;
    }

    private QueryBuilder getQuery(AssetSearch search) {
        /**
         * An empty boolean query is treated like a match all.
         */
        BoolQueryBuilder query = QueryBuilders.boolQuery();

        if (search.isQuerySet()) {
            query.must(getQueryStringQuery(search));
        }

        AssetFilter filter = search.getFilter();
        if (filter.getFolderIds() != null) {
            Set<String> folderIds = Sets.newHashSetWithExpectedSize(64);
            for (Folder folder : folderService.getAllDescendants(
                    folderService.getAll(filter.getFolderIds()), true)) {
                if (folder.getSearch() != null) {
                    query.must(getQuery(folder.getSearch()));
                }
                folderIds.add(folder.getId());
            }

            if (!folderIds.isEmpty()) {
                query.should(QueryBuilders.termsQuery("folders", folderIds));
            }
        }

        return QueryBuilders.filteredQuery(query, getFilter(filter));
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {
        QueryStringQueryBuilder query = QueryBuilders.queryStringQuery(search.getQuery());
        if (search.getConfidence() <= 0) {
            query.field("keywords.all.raw", 1);
            query.field("keywords.all");
        }
        else {
            long highBucket = KeywordsSchema.getBucket(search.getConfidence());
            for (long i=highBucket; i<=KeywordsSchema.BUCKET_COUNT; i++) {
                query.field(String.format("keywords.level%d.raw", i), i + 1);
                query.field(String.format("keywords.level%d", i));
            }
        }
        query.lenient(true);
        return  query;
    }


    /**
     * Builds an "AND" filter based on all the options in the AssetFilter.
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

        if (builder.isSelected()) {
            // FIXME: Refactor avoid race conditions, add room endpoints to get selected set atomically
            Room room = roomService.getActiveRoom(userService.getActiveSession());
            if (room != null) {
                TermFilterBuilder selectedBuilder = FilterBuilders.termFilter("selectedRooms", room.getId());
                filter.add(selectedBuilder);
            }
        }

        if (builder.getAssetIds() != null) {
            FilterBuilder assetsFilterrBuilder = FilterBuilders.termFilter("_id", builder.getAssetIds());
            filter.add(assetsFilterrBuilder);
        }

        if (builder.getExportIds() != null) {
            for (Integer id : builder.getExportIds()) {
                FilterBuilder exportFilterBuilder = FilterBuilders.termFilter("exports", id);
                filter.add(exportFilterBuilder);
            }
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
                FilterBuilder scriptFilterBuilder = FilterBuilders.scriptFilter(script.getScript())
                        .lang("native")
                        .params(script.getParams());
                filter.add(scriptFilterBuilder);
            }
        }

        return filter;
    }
}
