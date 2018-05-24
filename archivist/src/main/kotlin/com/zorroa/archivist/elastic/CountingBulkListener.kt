package com.zorroa.archivist.elastic

import org.elasticsearch.action.bulk.BulkProcessor
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.LongAdder

/**
 * Created by chambers on 6/22/17.
 */
class CountingBulkListener : BulkProcessor.Listener {

    val successCount = LongAdder()
    val errorCount = LongAdder()

    override fun beforeBulk(l: Long, bulkRequest: BulkRequest) {

    }

    override fun afterBulk(l: Long, bulkRequest: BulkRequest, bulkResponse: BulkResponse) {
        successCount.add(bulkResponse.items.size.toLong())
    }

    override fun afterBulk(l: Long, bulkRequest: BulkRequest, throwable: Throwable) {
        errorCount.increment()
        logger.warn("Failed to process bulk request: ", throwable)
    }

    fun getSuccessCount(): Long {
        return successCount.toLong()
    }

    fun getErrorCount(): Long {
        return errorCount.toLong()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(CountingBulkListener::class.java)
    }
}
