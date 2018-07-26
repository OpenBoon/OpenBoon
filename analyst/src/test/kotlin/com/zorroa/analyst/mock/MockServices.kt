package com.zorroa.analyst.mock

import com.zorroa.analyst.service.JobStorageService
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.server.mock.KubernetesServer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL

class MockStorageService: JobStorageService {

    var lastBlob : ByteArray? = null

    var url = URL("http://johnvansickle.com")

    override fun storeBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        logger.info("MockStorageService.storeBlob")
        lastBlob = bytes
        return url
    }

    override fun getSignedUrl(path: String): URL {
        logger.info("MockStorageService.getSignedUrl")
        return URL("http://johnvansickle.com?signed=true")
    }

    override fun getInputStream(path: String): InputStream {
        logger.info("MockStorageService.getInputStream")
        return ByteArrayInputStream(lastBlob)
    }

    override fun storeSignedBlob(path: String, mediaType: String, bytes: ByteArray): URL {
        logger.info("MockStorageService.storeSignedBlob")
        return url
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MockStorageService::class.java)
    }

}

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
    fun mockStorageService() : JobStorageService {
        logger.info("creating mock Storage Server")
        return MockStorageService()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MockServiceConfiguration::class.java)
    }

}

