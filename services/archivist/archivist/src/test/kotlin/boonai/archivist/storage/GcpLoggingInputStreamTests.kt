package boonai.archivist.storage

import com.google.cloud.MonitoredResource
import com.google.cloud.logging.LogEntry
import com.google.cloud.logging.Logging
import com.google.cloud.logging.LoggingOptions
import com.google.cloud.logging.Payload
import com.google.cloud.logging.Severity
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory

@Ignore
class GcpLoggingInputStreamTests {

    val logger = LoggerFactory.getLogger(GcpLoggingInputStreamTests::class.java)

    val logging: Logging = LoggingOptions.getDefaultInstance().service
    val logName = "integration-test-log"
    val logPath = "projects/zorroa-deploy/logs/integration-test-log"

    @Test
    fun setup() {

        // The data to write to the log
        val text = "Hello, world!"
        val entry: LogEntry = LogEntry.newBuilder(Payload.StringPayload.of(text))
            .setSeverity(Severity.INFO)
            .setLogName(logName)
            .setResource(MonitoredResource.newBuilder("global").build())
            .build()

        // Writes the log entry asynchronously
        logging.write(listOf(entry))
        System.out.printf("Logged: %s%n", text)
        Thread.sleep(5000)
    }

    @Test
    fun testRead() {
        val stream = GcpLogInputStream(logging, logPath)
        val res = String(stream.readAllBytes())
        logger.info(res)
    }
}
