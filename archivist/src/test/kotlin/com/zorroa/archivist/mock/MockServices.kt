package com.zorroa.archivist.mock

import com.zorroa.archivist.service.ObjectFile
import com.zorroa.archivist.service.PubSubService
import com.zorroa.archivist.service.StorageService
import com.zorroa.common.clients.AnalystClient
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.util.*

class MockAnalystClient: AnalystClient {

    var lastSpec : JobSpec? = null

    override fun createJob(spec: JobSpec): Job {
        lastSpec = spec
        logger.info("spec: {}", Json.prettyString(spec))
        return Job(UUID.randomUUID(), spec.type, spec.organizationId,
                spec.name, JobState.WAITING, true,
                spec.attrs, spec.env)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MockAnalystClient::class.java)
    }
}

class MockPubSubService : PubSubService {

}

class MockStorageService : StorageService {
    override fun getSignedUrl(url: URL): URL {
        val urlStr = url.toString()
        return URL("$urlStr?key=bar")
    }

    override fun getObjectFile(url: URL, mimeType: String?): ObjectFile {
        return ObjectFile(FileInputStream(File("../../zorroa-test-data/images/set01/faces.jpg")),
                "faces.jpg", 113333, "image/jpeg")
    }

    override fun objectExists(url: URL): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@Configuration
@Order(-1)
class MockServiceConfiguration {

    @Bean
    @Primary
    fun mockAnalystClient() : AnalystClient {
        return MockAnalystClient()
    }

    @Bean
    @Primary
    fun mockStroageService(): StorageService {
        return MockStorageService()
    }

    @Bean
    @Primary
    fun mockPubsubService(): PubSubService {
        return MockPubSubService()
    }
}
