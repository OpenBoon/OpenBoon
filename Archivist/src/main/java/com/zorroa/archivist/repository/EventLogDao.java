package com.zorroa.archivist.repository;

import com.zorroa.archivist.sdk.domain.EventLogSearch;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;

/**
 * Created by chambers on 12/28/15.
 */
public interface EventLogDao {

    SearchResponse getAll();

    /**
     * Execute a search in the event log.
     *
     * @param search
     * @return
     */
    SearchResponse getAll(EventLogSearch search);


    /**
     * Execute a count query in the event log.
     *
     * @param search
     * @return
     */
    CountResponse getCount(EventLogSearch search);
}
