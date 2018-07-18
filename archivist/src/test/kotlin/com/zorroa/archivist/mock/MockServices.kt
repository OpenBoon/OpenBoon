package com.zorroa.archivist.mock

import com.zorroa.archivist.service.ObjectFile
import com.zorroa.archivist.service.StorageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import java.io.File
import java.io.FileInputStream
import java.net.URL

class MockStorageService : StorageService {

    override fun getObjectFile(url: URL, mimeType: String?): ObjectFile {
        return ObjectFile(FileInputStream(File("../../zorroa-test-data/images/set01/faces.jpg")),
                "faces.jpg", 113333, "image/jpeg")
    }

    override fun objectExists(url: URL): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

@Profile("test")
@Configuration
@Order(-1)
class MockServiceConfiguration {

    @Bean
    @Primary
    fun mockStroageService(): StorageService {
        return MockStorageService()
    }
}
