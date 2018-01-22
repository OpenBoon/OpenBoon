package com.zorroa.archivist.repository

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import com.zorroa.archivist.domain.EventLogSearch
import com.zorroa.archivist.domain.TaskId
import com.zorroa.archivist.domain.UserLogSpec
import com.zorroa.cluster.thrift.TaskErrorT
import com.zorroa.common.elastic.AbstractElasticDao
import com.zorroa.common.elastic.SearchHitRowMapper
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import org.elasticsearch.action.index.IndexRequest
import org.springframework.stereotype.Repository

interface EventLogDao {
    fun getAll(type: String, search: EventLogSearch): PagedList<Map<String, Any>>

    fun create(spec: UserLogSpec)

    fun create(task: TaskId, errors: List<TaskErrorT>)
}

@Repository
open class EventLogDaoImpl : AbstractElasticDao(), EventLogDao {

    override fun getAll(type: String, search: EventLogSearch): PagedList<Map<String, Any>> {
        val builder = client.prepareSearch(type + index)
                .setQuery(search.query)
                .setAggregations(search.aggs)
        val page = Pager(search.from, search.size, 0)
        return elastic.page(builder, page, MAPPER)
    }

    override fun create(spec: UserLogSpec) {
        /**
         * Don't wait around for the log to even finish, its supposed to be
         * as async as possible.
         */
        val bytes = Json.serialize(spec)
        client.prepareIndex("user_logs", "entry")
                .setOpType(IndexRequest.OpType.INDEX)
                .setSource(bytes)
                .get()
    }

    override fun create(task: TaskId, errors: List<TaskErrorT>) {
        /**
         * Don't wait around for the log to even finish, its supposed to be
         * as async as possible.
         */
        val bulkRequest = client.prepareBulk()
        for (error in errors) {
            val entry = Maps.newHashMap<String, Any>()
            entry.put("taskId", task.taskId)
            entry.put("jobId", task.jobId)
            entry.put("message", error.getMessage())
            entry.put("service", error.getOriginService())
            entry.put("skipped", error.isSkipped)
            entry.put("path", error.getPath())
            entry.put("assetId", error.getId())
            entry.put("processor", error.getProcessor())
            entry.put("phase", error.getPhase())
            entry.put("timestamp", error.getTimestamp())

            val stackTrace = Lists.newArrayList<Map<String, Any>>()
            for (e in error.getStack()) {
                val stack = Maps.newHashMap<String, Any>()
                stack.put("className", e.getClassName())
                stack.put("file", e.getFile())
                stack.put("lineNumber", e.getLineNumber())
                stack.put("method", e.getMethod())
                stackTrace.add(stack)
            }
            entry.put("stackTrace", stackTrace)

            bulkRequest.add(client.prepareIndex("job_logs", "entry")
                    .setSource(entry))
        }

        val bulk = bulkRequest.get()
        if (bulk.hasFailures()) {
            logger.warn("Bulk job log failure, {}", bulk.buildFailureMessage())
        }
    }

    override fun getType(): String? {
        return null
    }

    override fun getIndex(): String {
        return "_logs"
    }

    companion object {

        private val MAPPER =  SearchHitRowMapper<Map<String,Any>> { hit ->
            val data = hit.source
            data.put("_id", hit.id)
            data.put("_score", hit.score)
            data
        }
    }
}
