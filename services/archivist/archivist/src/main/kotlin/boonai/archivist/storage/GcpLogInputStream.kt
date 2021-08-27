package boonai.archivist.storage

import com.google.api.gax.paging.Page
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import org.slf4j.LoggerFactory
import java.io.InputStream

class GcpLogInputStream(
    val loggingService: Logging,
    val logName: String
) : InputStream() {

    var page: Page<LogEntry>? = null
    val buffer: StringBuilder = StringBuilder(1024)
    var index = -1

    init {
        pullNextPage(null)
    }

    /**
     * Pulls a page of logs and caches the log output into a StringBuilder.
     */
    fun pullNextPage(curpage: Page<LogEntry>?): Boolean {
        page = if (curpage == null) {
            logger.info("pulling first page $curpage")
            loggingService.listLogEntries(Logging.EntryListOption.filter("logName=$logName"))
        } else if (curpage.hasNextPage()) {
            logger.info("pulling next page")
            curpage.nextPage
        } else {
            logger.info("no more pages")
            null
        }

        if (page == null) {
            return false
        }

        page?.let { ipage ->
            buffer.clear()
            index = -1
            ipage.values.forEach {
                buffer.append(it.getPayload<Payload.StringPayload>().data)
                buffer.append('\n')
            }
        }

        return true
    }

    override fun read(): Int {
        if (page == null) {
            return -1
        }

        index += 1
        if (index >= buffer.length) {
            if (!pullNextPage(page)) {
                return -1
            }
        }
        return try {
            return buffer.codePointAt(index)
        } catch (e: IndexOutOfBoundsException) {
            -1
        }
    }

    override fun close() {
        buffer.clear()
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcpLogInputStream::class.java)
    }
}
