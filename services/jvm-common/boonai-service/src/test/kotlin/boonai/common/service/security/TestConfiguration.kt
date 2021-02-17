package boonai.common.service.security

import boonai.common.service.storage.SystemStorageService
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
