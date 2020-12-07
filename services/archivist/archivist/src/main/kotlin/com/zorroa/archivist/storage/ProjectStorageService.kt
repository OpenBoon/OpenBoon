package com.zorroa.archivist.storage

import com.zorroa.archivist.domain.ArchivistException
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProjectDirLocator
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
import java.util.concurrent.TimeUnit

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
     * Get he native storage URL for a given directory
     */
    fun getNativeUri(locator: ProjectDirLocator): String

    /**
     * Get a signed URL for the given [ProjectStorageLocator].
     */
    fun getSignedUrl(
        locator: ProjectStorageLocator,
        forWrite: Boolean,
        duration: Long,
        unit: TimeUnit
    ): Map<String, Any>

    /**
     * Set a [Map] of arbitrary attrs for the given [ProjectStorageLocator].
     */
    fun setAttrs(locator: ProjectStorageLocator, attrs: Map<String, Any>): FileStorage

    /**
     * Delete all associated files stored against a particular entity.
     */
    fun recursiveDelete(locator: ProjectDirLocator)

    /**
     * Delete recursively ALL FILES below a certain path
     */
    fun recursiveDelete(path: String)

    /**
     * Log the storage of a file.
     */
    fun logStoreEvent(spec: ProjectStorageSpec) {
        logger.event(
            LogObject.PROJECT_STORAGE, LogAction.CREATE,
            mapOf(
                "newFilePath" to spec.locator.getPath(),
                "size" to spec.data.size.toLong(),
                "mediaType" to spec.mimetype
            )
        )
    }

    /**
     * Log the storage of a file.
     */
    fun logDeleteEvent(path: String) {
        logger.event(
            LogObject.PROJECT_STORAGE, LogAction.DELETE,
            mapOf(
                "path" to path
            )
        )
    }

    /**
     * Log the signing of a cloud storage URL.
     */
    fun logSignEvent(path: String, mediaType: String, forWrite: Boolean) {
        val action = if (forWrite) {
            LogAction.SIGN_FOR_WRITE
        } else {
            LogAction.SIGN_FOR_READ
        }

        logger.event(
            LogObject.PROJECT_STORAGE, action,
            mapOf(
                "mediaType" to mediaType,
                "path" to path
            )
        )
    }

    companion object {
        val logger = LoggerFactory.getLogger(ProjectStorageService::class.java)
    }
}

class ProjectStorageException(message: String, cause: Throwable) : ArchivistException(message, cause)
