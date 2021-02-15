package com.zorroa.zmlp.service.security

import com.zorroa.zmlp.service.storage.SystemStorageService
import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestConfiguration {

    @Bean
    fun encryptionService(): EncryptionService {
        return EncryptionServiceImpl()
    }

    @Bean
    fun systemStorageService(): SystemStorageService {
        return Mockito.mock(SystemStorageService::class.java)
    }
}
