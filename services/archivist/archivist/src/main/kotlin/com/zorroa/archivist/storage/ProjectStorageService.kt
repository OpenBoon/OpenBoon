package com.zorroa.archivist.storage

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.zmlp.service.logging.event
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
     * Fetch the URI where the file is stored.
     */
    fun getNativeUri(locator: ProjectStorageLocator): String

    /**
     * Delete all associated Asset files from storage server
     */
    fun deleteAsset(id: String)

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

    /**
     * Log the storage of a file.
     */
    fun logDeleteEvent(assetPath: String) {
        logger.event(
            LogObject.PROJECT_STORAGE, LogAction.DELETE,
            mapOf(
                "assetPath" to assetPath
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectStorageService::class.java)
    }
}
