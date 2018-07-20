package com.zorroa.analyst.mock

import com.zorroa.analyst.service.EventService
import com.zorroa.analyst.service.PipelineServiceImpl
import com.zorroa.analyst.service.StorageService
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import java.net.URL

class MockStorageService: StorageService {
    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        return URL("http://johnvansickle.com")
    }
}

class MockEventService: EventService {

}

@Profile("test")
@Configuration
@Order(-1)
class MockServiceConfiguration {

    @Bean
    @Primary
    fun mockKubernetesClient() : KubernetesClient {
        val server = KubernetesServer(true, true)
        server.before()
        return server.client
    }

    @Bean
    @Primary
    fun mockStorageService() : StorageService {
        logger.info("creating mock Storage Server")
        return MockStorageService()
    }

    @Bean
    @Primary
    fun mockEventService() : EventService {
        logger.info("creating mock event service")
        return MockEventService()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }

}

