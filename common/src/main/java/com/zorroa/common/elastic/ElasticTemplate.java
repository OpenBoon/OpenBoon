package com.zorroa.common.elastic;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Lists;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.search.Scroll;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.InternalAggregations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class ElasticTemplate {

    private static final Logger logger = LoggerFactory.getLogger(ElasticTemplate.class);


    private Client client;
    private String index;
    private String type;

    public ElasticTemplate(Client client, String index, String type) {
        this.client = client;
        this.index = index;
        this.type = type;
    }

    public <T> T queryForObject(String id, JsonRowMapper<T> mapper) {
        final GetRequestBuilder builder = client.prepareGet(index, type, id).setFetchSource(true);
        final GetResponse r = builder.get();
        if (!r.isExists()) {
            throw new EmptyResultDataAccessException(
                    "Expected 1 '" + type + "' of id '" + id + "'", 0);
        }
        try {
            return mapper.mapRow(r.getId(), r.getVersion(), 0, r.getSourceAsBytes());
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
        }
    }

    public <T> T queryForObject(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        if (r.getHits().getTotalHits() == 0) {
            throw new EmptyResultDataAccessException("Expected 1 result from " + builder.toString() + ", got: ", 0);
        }
        SearchHit hit = r.getHits().getAt(0);
        try {
            return mapper.mapRow(hit.getId(), hit.getVersion(), hit.score(), hit.source());
        } catch (Exception e) {
            throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
        }
    }

    public <T> List<T> query(JsonRowMapper<T> mapper) {
        final SearchResponse r = client.prepareSearch(index)
            .setQuery(QueryBuilders.matchAllQuery())
            .setTypes(type)
            .setIndices(index)
            .get();

        List<T> result = Lists.newArrayListWithCapacity((int)r.getHits().getTotalHits());
        for (SearchHit hit: r.getHits()) {
            try {
                result.add(mapper.mapRow(hit.getId(), hit.getVersion(),hit.score(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }
        return result;
    }

    public <T> PagedList<T> scroll(String id, String timeout, JsonRowMapper<T> mapper) {
        SearchScrollRequestBuilder ssrb = client.prepareSearchScroll(id).setScroll(timeout);
        final SearchResponse r = ssrb.get();

        final List<T> list = Lists.newArrayListWithCapacity(r.getHits().getHits().length);
        for (SearchHit hit: r.getHits().getHits()) {
            try {
                list.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.getScore(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }

        long totalCount = r.getHits().getTotalHits();
        PagedList result = new PagedList(new Pager().setTotalCount(totalCount), list);
        result.setScroll(new Scroll(r.getScrollId()));

        return result;
    }

    public <T> List<T> query(SearchRequestBuilder builder, JsonRowMapper<T> mapper) {
        final SearchResponse r = builder.get();
        List<T> result = Lists.newArrayListWithCapacity((int) r.getHits().getTotalHits());

        for (SearchHit hit : r.getHits()) {
            try {
                result.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.getScore(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }

        return result;
    }

    public <T> PagedList<T> page(SearchRequestBuilder builder, Pager paging, JsonRowMapper<T> mapper) {
        builder.setSize(paging.getSize()).setFrom(paging.getFrom());

        final SearchResponse r = builder.get();
        final List<T> list = Lists.newArrayListWithCapacity(r.getHits().getHits().length);
        for (SearchHit hit: r.getHits().getHits()) {
            try {
                list.add(mapper.mapRow(hit.getId(), hit.getVersion(), hit.getScore(), hit.source()));
            } catch (Exception e) {
                throw new DataRetrievalFailureException("Failed to parse record, " + e, e);
            }
        }

        paging.setTotalCount(r.getHits().getTotalHits());
        PagedList result = new PagedList(paging, list);
        result.setScroll(new Scroll(r.getScrollId()));

        if (r.getAggregations() != null) {
            try {
                InternalAggregations aggregations = (InternalAggregations) r.getAggregations();
                XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
                jsonBuilder.startObject();
                aggregations.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);
                jsonBuilder.endObject();
                result.setAggregations(Json.Mapper.readValue(jsonBuilder.string(), Json.GENERIC_MAP));
            } catch (IOException e) {
                logger.warn("Failed to deserialize aggregations.", e);
            }
        }

        return result;
    }

    public void page(SearchRequestBuilder builder, Pager paging, JsonRowMapper<?> mapper, OutputStream out) throws IOException {

        builder.setSize(paging.getSize()).setFrom(paging.getFrom());
        final SearchResponse r = builder.get();

        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(out, JsonEncoding.UTF8);
        generator.setCodec(Json.Mapper);
        generator.writeStartObject();
        generator.writeArrayFieldStart("list");

        for (SearchHit hit: r.getHits().getHits()) {
            generator.writeStartObject();
            try {
                generator.writeStringField("id", hit.getId());
                generator.writeStringField("type", hit.getType());
                generator.writeNumberField("score", hit.score());
                generator.writeObjectField("document", hit.getSource());

            } catch (Exception e) {
                // Can't really let this escape since we're streaming directly to client.
                logger.warn("Failed to parse record {}", e.getMessage(), e);
            }
            finally {
                generator.writeEndObject();

            }
        }
        generator.writeEndArray();

        paging.setTotalCount(r.getHits().getTotalHits());
        generator.writeObjectField("page", paging);

        if (r.getAggregations() != null) {
            try {
                InternalAggregations aggregations = (InternalAggregations) r.getAggregations();
                XContentBuilder jsonBuilder = XContentFactory.jsonBuilder();
                aggregations.toXContent(jsonBuilder, ToXContent.EMPTY_PARAMS);

                String json = jsonBuilder.string();

                generator.writeRaw(",");
                generator.writeRaw("\"aggregations\":");
                generator.writeRaw(json.substring(14));

            } catch (IOException e) {
                logger.warn("Failed to deserialize aggregations.", e);
            }
        }

        generator.writeEndObject();
        generator.flush();
        generator.close();
    }

    /**
     * Return the count for the given search.
     *
     * @param builder
     * @return
     */
    public long count(SearchRequestBuilder builder) {
        builder.setSize(0);
        final SearchResponse r = builder.get();
        return r.getHits().getTotalHits();
    }

    /**
     * Index the given IndexRequestBuilder and return the document id.
     * @param builder
     * @return
     */
    public String index(IndexRequestBuilder builder) {
        return builder.get().getId();
    }
}
