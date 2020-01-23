package com.zorroa.archivist.storage

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.event
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

@Configuration
@ConfigurationProperties("zmlp.storage.project")
class StorageProperties {

    lateinit var bucket: String

    var createBucket: Boolean = false

    var accessKey: String? = null

    var secretKey: String? = null

    var url: String? = null
}

/**
 * The FileStorageService handles storing files to bucket storage
 * using a standard naming convention defined by [ProjectStorageLocator]
 * class.
 */
interface ProjectStorageService {

    /**
     * Store the file described by the [ProjectStorageSpec] in bucket storage.
     */
    fun store(spec: ProjectStorageSpec): FileStorage

    /**
     * Stream the given file as a ResponseEntity.  This is used for serving
     * the resource via the HTTP server.
     */
    fun stream(locator: ProjectStorageLocator): ResponseEntity<Resource>

    /**
     * Fetch the bytes for the given file.
     */
    fun fetch(locator: ProjectStorageLocator): ByteArray

    /**
     * Log the storage of a file.
     */
    fun logStoreEvent(spec: ProjectStorageSpec) {
        logger.event(
            LogObject.PROJECT_STORAGE, LogAction.CREATE,
            mapOf(
                "newFilePath" to spec.locator.getPath(),
                "size" to spec.data.size.toLong(),
                "mimetype" to spec.mimetype
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectStorageService::class.java)
    }
}
