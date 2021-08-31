package boonai.archivist.storage

import com.google.cloud.logging.Logging
import com.google.cloud.logging.Payload
import org.slf4j.LoggerFactory
import java.io.InputStream

class GcpLogInputStream(
    val loggingService: Logging,
    val logName: String,
) : InputStream() {

    // var page: Page<LogEntry>? = null
    val buffer: StringBuilder = StringBuilder(4096)
    var index = -1

    init {
        var entries = loggingService.listLogEntries(
            Logging.EntryListOption.filter("logName=$logName"),
            Logging.EntryListOption.pageSize(200),
            Logging.EntryListOption.sortOrder(Logging.SortingField.TIMESTAMP, Logging.SortingOrder.DESCENDING)
        )

        for (entry in entries.values.reversed()) {
            val payload = entry.getPayload<Payload.JsonPayload>().dataAsMap.getOrDefault("message", "").toString()
            buffer.append(payload)
            buffer.append('\n')
        }
    }

    override fun read(): Int {
        index += 1
        if (index >= buffer.length) {
            return -1
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
