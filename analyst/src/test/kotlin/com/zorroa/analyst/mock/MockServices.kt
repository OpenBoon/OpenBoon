package com.zorroa.analyst.mock

import com.zorroa.common.domain.IndexRoute
import com.zorroa.analyst.rest.IndexRouteClient
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
import java.util.*

class MockStorageService: StorageService {
    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        return URL("http://johnvansickle.com")
    }
}

class MockEventService: EventService {

}


class MockIndexRouteClient : IndexRouteClient {

    override fun getIndexRoute(orgId: UUID) : IndexRoute {
        return IndexRoute(UUID.fromString("00000000-9998-8888-7777-666666666666"),
                "100",
                "http://localhost:9200",
                "zorroa_v10",
                null)
    }
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

    @Bean
    @Primary
    fun mockIndexRouteClient(): IndexRouteClient{
        return MockIndexRouteClient()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }

}

