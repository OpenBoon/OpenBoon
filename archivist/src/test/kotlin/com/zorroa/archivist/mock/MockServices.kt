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
