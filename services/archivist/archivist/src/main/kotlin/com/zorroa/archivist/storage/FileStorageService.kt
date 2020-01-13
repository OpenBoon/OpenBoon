package com.zorroa.archivist.storage

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.CloudStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.service.event
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

@Configuration
@ConfigurationProperties("archivist.storage")
class StorageProperties {

    lateinit var bucket: String

    var createBucket: Boolean = false

    var accessKey: String? = null

    var secretKey: String? = null

    var url: String? = null
}

interface FileStorageService {

    /**
     * Store the file described by the [FileStorageSpec] in bucket storage.
     */
    fun store(spec: FileStorageSpec): FileStorage

    /**
     * Stream the given file as a ResponseEntity.
     */
    fun stream(locator: CloudStorageLocator): ResponseEntity<Resource>

    /**
     * Fetch the bytes for the given file.
     */
    fun fetch(locator: CloudStorageLocator): ByteArray

    /**
     * Log the storage of a file.
     */
    fun logStoreEvent(spec: FileStorageSpec ) {
        logger.event(
            LogObject.FILE_STORAGE, LogAction.CREATE,
            mapOf(
                "newFilePath" to spec.locator.getPath(),
                "size" to spec.data.size.toLong(),
                "mimetype" to spec.mimetype
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(FileStorageService::class.java)
    }
}
