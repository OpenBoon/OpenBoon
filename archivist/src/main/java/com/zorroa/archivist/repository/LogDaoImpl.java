package com.zorroa.archivist.repository;

import com.zorroa.archivist.domain.LogSearch;
import com.zorroa.archivist.domain.LogSpec;
import com.zorroa.common.domain.Paging;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.ElasticPagedList;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * The event log is a temporary/rotating log of events which are time sensitive.
 * They are kept around for a default of 7 days.
 */
@Repository
public class LogDaoImpl extends AbstractElasticDao implements LogDao {

    @Autowired
    Client client;

    @Override
    public String getType() {
        return "logs";
    }

    @Override
    public String getIndex() {
        return "eventlog";
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
    public ElasticPagedList<Map<String,Object>> search(LogSearch search, Paging page) {

        SearchRequestBuilder req = client.prepareSearch("eventlog")
                .setQuery(Json.serializeToString(search.getQuery(), "{\"match_all\": {}}"))
                .addSort("timestamp", SortOrder.DESC);

        if (search.getAggs() != null) {
            req.setAggregations(search.getAggs());
        }

        ElasticPagedList<Map<String,Object>> result =  elastic.page(req, page,
                    (id, version, source) -> {
                        Map<String,Object> r =  Json.deserialize(source, Json.GENERIC_MAP);
                        r.put("id", id);
                        return r;
                    });
        return result;
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
