package com.zorroa.archivist.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zorroa.archivist.domain.EventLogSearch;
import com.zorroa.archivist.domain.TaskId;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.common.cluster.thrift.StackElementT;
import com.zorroa.common.cluster.thrift.TaskErrorT;
import com.zorroa.common.elastic.AbstractElasticDao;
import com.zorroa.common.elastic.SearchHitRowMapper;
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

    private static final SearchHitRowMapper<Map<String,Object>> MAPPER = (hit) -> {
        Map<String, Object> data = hit.getSource();
        data.put("_id", hit.getId());
        data.put("_score", hit.getScore());
        return data;
    };

    @Override
    public PagedList<Map<String,Object>> getAll(String type, EventLogSearch search) {
        SearchRequestBuilder builder = client.prepareSearch(type + getIndex() )
                .setQuery(search.getQuery())
                .setAggregations(search.getAggs());
        Pager page = new Pager(search.getFrom(), search.getSize(), 0);
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

            List<Map<String,Object>> stackTrace = Lists.newArrayList();
            for (StackElementT e: error.getStack()) {
                Map<String,Object> stack = Maps.newHashMap();
                stack.put("className", e.getClassName());
                stack.put("file", e.getFile());
                stack.put("lineNumber", e.getLineNumber());
                stack.put("method", e.getMethod());
                stackTrace.add(stack);
            }
            entry.put("stackTrace", stackTrace);

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
