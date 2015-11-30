package com.zorroa.archivist.service;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.ScanAndScrollAssetIterator;
import com.zorroa.archivist.repository.PermissionDao;
import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.service.FolderService;
import com.zorroa.archivist.sdk.service.RoomService;
import com.zorroa.archivist.sdk.service.UserService;
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
        QueryBuilder query;

        if (search.getQuery() != null) {
            query = getQueryStringQuery(search);
        } else {
            query = QueryBuilders.matchAllQuery();
        }

        return QueryBuilders.filteredQuery(query, getFilter(search.getFilter()));
    }

    private QueryBuilder getQueryStringQuery(AssetSearch search) {
        QueryStringQueryBuilder query = QueryBuilders.queryStringQuery(search.getQuery());
        if (search.getConfidence() <= 0) {
            query.field("keywords.all.raw", 1);
            query.field("keywords.all");
        }
        else {
            for (int i = 5; i >= search.getConfidence(); i--) {
                query.field(String.format("keywords.level%d.raw", i), i + 1);
                query.field(String.format("keywords.level%d", i));
            }
        }
        query.lenient(true);
        return query;
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
            Room room = roomService.getActiveRoom(userService.getActiveSession());
            if (room != null) {
                TermFilterBuilder selectedBuilder = FilterBuilders.termFilter("selectedRooms", room.getId());
                filter.add(selectedBuilder);
            }
        }

        if (builder.getFolderIds() != null) {
            filter.add(getFolderFilter(builder));
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

    public FilterBuilder getFolderFilter(AssetFilter builder) {
        return FilterBuilders.termsFilter("folders", folderService.getAllDecendentIds(builder.getFolderIds()));
    }
}
