package com.zorroa.archivist.mock

import com.zorroa.archivist.service.PubSubService
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

    override fun <T> get(url: String, resultType: Class<T>, jwtClaims: Map<String, String>?): T {
        var text = "null"
        if (url == "/api/v1/processor-lists/defaults/export") {
            text = """{"name": "default-export","processors": [{"className": "zplugins.export.PdfProcessor","language": "python","args": {}}]}"""
        }
        return Json.Mapper.readValue(text, resultType)

    }

    companion object {

        private val logger = LoggerFactory.getLogger(MockAnalystClient::class.java)
    }
}

class MockPubSubService : PubSubService {

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
    fun mockPubsubService(): PubSubService {
        return MockPubSubService()
    }
}
