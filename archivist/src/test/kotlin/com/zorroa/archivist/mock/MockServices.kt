package com.zorroa.archivist.mock

import com.nhaarman.mockito_kotlin.mock
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.PubSubService
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

class MockPubSubService : PubSubService

/**
 * Replacement mockito.any method that handles methods that
 * don't accept null.  Casts an Any object to the given type.
 */
fun <T> zany(clazz: Class<T>) : T {
    val any: Any? = Mockito.any()
    return clazz.cast(any)

}


/**
 * Replacement mockito.any method that handles methods that
 * don't accept null.  Casts an Any object to an inferred type.
 */
fun <T> zany() : T {
    val any: Any? = Mockito.any()
    return any as T

}

@Configuration
@Order(-1)
class MockServiceConfiguration {

    @Bean
    @Primary
    fun mockPubsubService(): PubSubService {
        return MockPubSubService()
    }

    /**
     * Return a mock FileStorageService if FileStorage is setup to use GCS.
     */
    @Bean
    @ConditionalOnProperty(prefix = "archivist", name=["storage.type"], havingValue = "gcs")
    @Primary
    @Autowired
    fun mockFileStorageService(): FileStorageService {
        return mock()
    }

    /**
     * Return a mock work queue which runs all tasks in the current thread.
     */
    @Bean
    @Primary
    fun mockWorkQueue() : AsyncListenableTaskExecutor {
        return MockAsyncThreadExecutor()
    }

}
