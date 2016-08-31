package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.PagedElasticList;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The event log is a temporary/rotating log of events which are time sensitive.
 * They are kept around for a default of 7 days.
 */
@Repository
public class LogDaoImpl extends AbstractElasticDao implements LogDao {

    protected final Logger logger = LoggerFactory.getLogger(LogDaoImpl.class);

    @Autowired
    Client client;

    @Override
    public String getType() {
        return "logs";
    }

    @Override
    public void create(LogSpec spec) {
        /**
         * Don't wait around for the log to even finish, its supposed to be
         * as async as possible.
         */
        byte[] bytes = Json.serialize(spec);
        client.prepareIndex(getIndexName(spec), getType())
                    .setOpType(IndexRequest.OpType.INDEX)
                    .setSource(bytes)
                    .get();
    }

    @Override
    public PagedElasticList search(LogSearch search, Paging page) {
        return new PagedElasticList(client.prepareSearch("eventlog")
                .setQuery(Json.serializeToString(search.getQuery(), "{\"match_all\": {}}"))
                .setFrom(page.getFrom())
                .setSize(page.getSize())
                .addSort("timestamp", SortOrder.DESC).get(), page);
    }

    @Override
    public String getIndexName(LogSpec spec) {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(new Date(spec.getTimestamp()));

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH); /* 0 through 11 */
        int quarter = (month / 3) + 1;

        StringBuilder sb = new StringBuilder(32);
        sb.append("eventlog_");
        sb.append(year);
        sb.append("_q");
        sb.append(quarter);
        return sb.toString();
    }
}
