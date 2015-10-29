package com.zorroa.archivist.service;

import com.zorroa.archivist.SecurityUtils;
import com.zorroa.archivist.domain.AssetSearchBuilder;
import com.zorroa.archivist.repository.PermissionDao;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
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

    @Value("${archivist.index.alias}")
    private String alias;

    @Autowired
    Client client;

    @Override
    public SearchResponse search(AssetSearchBuilder builder) {
        return buildSearch(builder).get();
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

        filter.add(getPermissionsFilter());

        return filter;
    }


    @Override
    public FilterBuilder getPermissionsFilter() {
        OrFilterBuilder result = FilterBuilders.orFilter();
        MissingFilterBuilder part1 = FilterBuilders.missingFilter("permissions.search");
        TermsFilterBuilder part2 = FilterBuilders.termsFilter("permissions.search",
                SecurityUtils.getPermissionIds());

        result.add(part1);
        result.add(part2);
        return result;
    }

}
