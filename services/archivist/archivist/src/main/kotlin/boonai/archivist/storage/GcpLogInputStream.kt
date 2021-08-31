package boonai.archivist.storage

import com.google.api.gax.paging.Page
import com.google.cloud.ServiceOptions
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import org.slf4j.LoggerFactory
import java.io.InputStream

class GcpLogInputStream(
    val loggingService: Logging,
    logName: String,
    logPath: String? = null
) : InputStream() {

    var page: Page<LogEntry>? = null
    val buffer: StringBuilder = StringBuilder(4096)
    var index = -1
    val fullLogName: String

    init {
        pullNextPage(null)

        fullLogName = if (logPath != null) {
            "$logPath/$logName"
        } else {
            val gcpProj = ServiceOptions.getDefaultProjectId() ?: "localdev"
            "projects/$gcpProj/logs/$logName"
        }
    }

    /**
     * Pulls a page of logs and caches the log output into a StringBuilder.
     */
    fun pullNextPage(curpage: Page<LogEntry>?): Boolean {
        page = if (curpage == null) {
            loggingService.listLogEntries(Logging.EntryListOption.filter("logName=$fullLogName"))
        } else if (curpage.hasNextPage()) {
            curpage.nextPage
        } else {
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
        return buffer.codePointAt(index)
    }

    override fun close() {
        buffer.clear()
    }

    companion object {
        val logger = LoggerFactory.getLogger(GcpLogInputStream::class.java)
    }
}
