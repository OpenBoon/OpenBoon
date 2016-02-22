package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.zorroa.archivist.sdk.domain.EventLogSearch;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * The event log is a temporary/rotating log of events which are time sensitive.
 * They are kept around for a default of 7 days.
 */
@Repository
public class EventLogDaoImpl implements EventLogDao {

    protected final Logger logger = LoggerFactory.getLogger(EventLogDaoImpl.class);

    @Autowired
    Client client;

    @Override
    public SearchResponse getAll() {
        return getAll(new EventLogSearch());
    }

    @Override
    public SearchResponse getAll(EventLogSearch search) {
        return client.prepareSearch("eventlog")
                .setQuery(getQuery(search))
                .setSize(search.getLimit())
                .setFrom((search.getPage() -1) * (search.getLimit()))
                .addSort("timestamp", SortOrder.DESC)
                .get();
    }

    @Override
    public CountResponse getCount(EventLogSearch search) {
        return client.prepareCount("eventlog")
                .setQuery(getQuery(search))
                .get();
    }

    public QueryBuilder getQuery(EventLogSearch search) {
        QueryBuilder query;

        if (search.getMessage() != null) {
            query = QueryBuilders.simpleQueryStringQuery(search.getMessage())
                    .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
        } else {
            query = QueryBuilders.matchAllQuery();
        }

        return QueryBuilders.filteredQuery(query, getFilter(search));
    }

    public FilterBuilder getFilter(EventLogSearch search) {
        List<FilterBuilder> filters = Lists.newArrayList();

        if (search.getIds() != null) {
            filters.add(FilterBuilders.termsFilter("id", search.getIds()));
        }

        if (search.getPath() != null) {
            filters.add(FilterBuilders.termFilter("path", search.getPath()));
        }

        if (search.getTags() != null) {
            filters.add(FilterBuilders.termsFilter("tags", search.getTags()));
        }

        if (search.getTypes() != null) {
            filters.add(FilterBuilders.termsFilter("type", search.getTypes()));
        }

        if (search.getAfterTime() > -1 || search.getBeforeTime() > -1) {
            RangeFilterBuilder range = FilterBuilders.rangeFilter("timestamp");
            if (search.getAfterTime() > -1) {
                range.gte(search.getAfterTime());
            }

            if (search.getBeforeTime() > -1) {
                range.lt(search.getBeforeTime());
            }
            filters.add(range);
        }

        return FilterBuilders.andFilter(filters.toArray(new FilterBuilder[] {}));
    }
}
