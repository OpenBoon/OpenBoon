package com.zorroa.archivist.elastic

import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse

import java.util.concurrent.atomic.LongAdder

/**
 * Created by chambers on 6/22/17.
 */
class CountingBulkListener : BulkProcessor.Listener {

    private val successCount = LongAdder()
    private val errorCount = LongAdder()

    override fun beforeBulk(l: Long, bulkRequest: BulkRequest) {

    }

    override fun afterBulk(l: Long, bulkRequest: BulkRequest, bulkResponse: BulkResponse) {
        successCount.add(bulkResponse.items.size.toLong())
    }

    override fun afterBulk(l: Long, bulkRequest: BulkRequest, throwable: Throwable) {
        errorCount.increment()
    }

    fun getSuccessCount(): Long {
        return successCount.toLong()
    }

    fun getErrorCount(): Long {
        return errorCount.toLong()
    }
}
