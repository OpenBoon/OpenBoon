package com.zorroa.archivist.mock

import com.zorroa.archivist.service.PubSubService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order

class MockPubSubService : PubSubService

@Configuration
@Order(-1)
class MockServiceConfiguration {

    @Bean
    @Primary
    fun mockPubsubService(): PubSubService {
        return MockPubSubService()
    }
}
