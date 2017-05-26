package com.zorroa.archivist.repository;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.TaskId;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.JsonRowMapper;
import com.zorroa.sdk.domain.PagedList;
import com.zorroa.sdk.domain.Pager;
import com.zorroa.sdk.util.Json;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Created by chambers on 5/19/17.
 */
@Repository
public class EventLogDaoImpl extends AbstractElasticDao implements EventLogDao {

    private static final JsonRowMapper<Map<String,Object>> MAPPER = (id, version, score, source) -> {
        Map<String, Object> data = Json.deserialize(source, Json.GENERIC_MAP);
        data.put("_id", id);
        data.put("_score", score);
        return data;
    };

    @Override
    public PagedList<Map<String,Object>> getAll(String type, EventLogSearch search, Pager page) {
        SearchRequestBuilder builder = client.prepareSearch(type + getIndex() )
                .setQuery(search.getQuery())
                .setAggregations(search.getAggs());

        return elastic.page(builder, page, MAPPER);
    }

    @Override
    public void create(UserLogSpec spec) {
        /**
         * Don't wait around for the log to even finish, its supposed to be
         * as async as possible.
         */
        byte[] bytes = Json.serialize(spec);
        client.prepareIndex("user_logs", "entry")
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(bytes)
                .get();
    }

    @Override
    public void create(TaskId task, List<TaskErrorT> errors) {
        /**
         * Don't wait around for the log to even finish, its supposed to be
         * as async as possible.
         */
        BulkRequestBuilder bulkRequest = client.prepareBulk();
        for (TaskErrorT error: errors) {
            Map<String, Object> entry = Maps.newHashMap();
            entry.put("taskId", task.getTaskId());
            entry.put("jobId", task.getJobId());
            entry.put("message", error.getMessage());
            entry.put("service", error.getOriginService());
            entry.put("skipped", error.isSkipped());
            entry.put("path", error.getPath());
            entry.put("assetId", error.getId());
            entry.put("processor", error.getProcessor());
            entry.put("phase", error.getPhase());
            entry.put("timestamp", error.getTimestamp());

            Map<String,Object> stack = Maps.newHashMap();
            stack.put("className", error.getClassName());
            stack.put("file", error.getFile());
            stack.put("lineNumber", error.getLineNumber());
            stack.put("method", error.getMethod());
            entry.put("stackTrace", ImmutableList.of(stack));

            bulkRequest.add(client.prepareIndex("job_logs", "entry")
                    .setSource(entry));
        }

        BulkResponse bulk = bulkRequest.get();
        if (bulk.hasFailures()) {
            logger.warn("Bulk job log failure, {}", bulk.buildFailureMessage());
        }
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public String getIndex() {
        return "_logs";
    }
}
