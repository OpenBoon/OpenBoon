package com.zorroa.common.repository;

import com.google.common.collect.Maps;
import com.zorroa.common.domain.*;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * The event log is a temporary/rotating log of events which are time sensitive.
 * They are kept around for a default of 7 days.
 */
@Repository
public class EventLogDaoImpl extends AbstractElasticDao implements EventLogDao {

    protected final Logger logger = LoggerFactory.getLogger(EventLogDaoImpl.class);
    @Autowired
    Client client;

    @Override
    public String getType() {
        return "event";
    }

    private static final JsonRowMapper<Event> MAPPER = (id, version, source) -> {
        Event event = Json.deserialize(source, Event.class);
        event.setId(id);
        return event;
    };

    @Override
    public PagedList<Event> getAll(EventSearch search) {
        return getAll(search, Paging.first());
    }

    @Override
    public PagedList<Event> getAll(Paging page) {
        return getAll(new EventSearch(), page);
    }

    @Override
    public PagedList<Event> getAll(EventSearch search, Paging paging) {
        return elastic.page(client.prepareSearch("eventlog")
                .setQuery(getQuery(search))
                .setSize(search.getLimit())
                .setTypes(getType())
                .setFrom((search.getPage() -1) * (search.getLimit()))
                .addSort("timestamp", SortOrder.DESC), paging, MAPPER);
    }

    @Override
    public long count(EventSearch search) {
        return client.prepareSearch("eventlog")
                .setQuery(getQuery(search))
                .setSize(0)
                .get().getHits().getTotalHits();
    }

    public QueryBuilder getQuery(EventSearch search) {
        QueryBuilder query;

        if (search.getMessage() != null) {
            query = QueryBuilders.simpleQueryStringQuery(search.getMessage())
                    .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
        } else {
            query = QueryBuilders.matchAllQuery();
        }

        return QueryBuilders.filteredQuery(query, getFilter(search));
    }

    public QueryBuilder getFilter(EventSearch search) {
        AndQueryBuilder result = QueryBuilders.andQuery();

        if (search.getObjectIds() != null) {
            result.add(QueryBuilders.termsQuery("objectId", search.getObjectIds()));
        }

        if (search.getPath() != null) {
            result.add(QueryBuilders.termQuery("path", search.getPath()));
        }

        if (search.getTags() != null) {
            result.add(QueryBuilders.termsQuery("tags", search.getTags()));
        }

        if (search.getObjectTypes() != null) {
            result.add(QueryBuilders.termsQuery("objectType", search.getObjectTypes()));
        }

        if (search.getAfterTime() > -1 || search.getBeforeTime() > -1) {
            RangeQueryBuilder range = QueryBuilders.rangeQuery("timestamp");
            if (search.getAfterTime() > -1) {
                range.gte(search.getAfterTime());
            }

            if (search.getBeforeTime() > -1) {
                range.lt(search.getBeforeTime());
            }
            result.add(range);
        }

        return result;
    }

    private String hostname;
    private boolean synchronous = false;

    @PostConstruct
    public void init() {
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
        }
    }

    public boolean isSynchronous() {
        return synchronous;
    }

    @Override
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    @Override
    public void info(EventSpec msg) {
        logger.info("{}", msg);
        create(msg, Level.INFO);
    }

    @Override
    public void warn(EventSpec msg) {
        logger.warn("{}", msg);
        create(msg, Level.WARN);
    }

    @Override
    public void error(EventSpec msg) {
        logger.error("{}", msg);
        create(msg, Level.ERROR);
    }

    private void create(EventSpec event, Level level) {
        Map<String, Object> source = Maps.newHashMap();
        source.put("level", level.toString());
        source.put("objectId", event.getObjectId() == null ? hostname : event.getObjectId());
        source.put("objectType", event.getObjectType() == null ? "server" : event.getObjectType());
        source.put("timestamp", event.getTimestamp());
        source.put("message", event.getMessage());
        source.put("tags", event.getTags());
        source.put("stack", getStackTrace(event.getException()));
        source.put("host", hostname);

        String date = new SimpleDateFormat("yyyy").format(new Date(event.getTimestamp()));
        String str = Json.serializeToString(source);

        IndexRequestBuilder builder = client.prepareIndex("eventlog_" + date, "event")
                .setOpType(IndexRequest.OpType.INDEX).setSource(str);

        if (synchronous) {
            builder.get();
        } else {
            builder.execute();
        }
    }

    static String[] getStackTrace(Throwable ex) {
        if (ex == null) {
            return null;
        }

        StackTraceElement[] e = ex.getStackTrace();
        final int length = e.length;

        String[] stack = new String[length];

        for (int i = 0; i < length; i++) {
            stack[i] = MessageFormatter.arrayFormat("{}.{} {}(line:{})",
                    new Object[]{
                            e[i].getClassName(),
                            e[i].getFileName(),
                            e[i].getMethodName(),
                            e[i].getLineNumber()
                    }).getMessage();
        }
        return stack;
    }
}
