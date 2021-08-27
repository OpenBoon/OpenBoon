package boonai.archivist.storage

import boonai.archivist.domain.ProjectStorageLocator
import com.google.api.gax.paging.Page
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import java.io.InputStream

class GcpLogInputStream(
    val loggingService: Logging,
    val locator: ProjectStorageLocator
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
            loggingService.listLogEntries(Logging.EntryListOption.filter(locator.name))
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
        index += 1
        if (index >= buffer.length) {
            if (!pullNextPage(page)) {
                return 0
            }
        }
        return try {
            Character.getNumericValue(buffer[index])
        } catch (e: IndexOutOfBoundsException) {
            0
        }
    }

    override fun close() {
        buffer.clear()
        page = null
    }
}
