package boonai.common.service.storage

import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("boonai.storage.system")
class SystemStorageProperties : StorageProperties()

/**
 * A service for storing JSON serialized blobs of configuration data for project
 * or system specific purposes.  This service uses a different bucket than
 * than [ProjectStorageService] to provide isolation between internally used files
 * and customer project file.
 */
interface SystemStorageService {

    val properties: SystemStorageProperties

    /**
     * Serialize the given object to JSON and store at the given path in the
     * system bucket.
     */
    fun storeObject(path: String, any: Any)

    /**
     * Fetch the given path and marshall the data into the specified type.
     */
    fun <T> fetchObject(path: String, valueType: Class<T>): T

    /**
     * Fetch the given path and marshall the data into the specified type.
     */
    fun <T> fetchObject(path: String, valueType: TypeReference<T>): T

    /**
     * Delete all associated files below a path.
     */
    fun recursiveDelete(path: String)
}
