package com.zorroa.archivist.service

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.filesystem.ObjectFileSystem
import com.zorroa.archivist.filesystem.OfsFile
import com.zorroa.archivist.security.getOrgId
import org.apache.tika.Tika
import org.springframework.beans.factory.annotation.Autowired
import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private val tika = Tika()
private val uuid3 = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

/**
 * The FileStorageService is for storing files associated with assets.  This
 * is typically used for proxies.
 *
 */
interface FileStorageService {
    /**
     * Allocates new file storage.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun create(spec: FileStorageSpec) : FileStorage

    /**
     * Use a FileStorageSpec to determine if a file already exists with the given spec.
     *
     * @param[spec] The FileStorageSpec which describes what is being stored.
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(spec: FileStorageSpec) : FileStorage

    /**
     * Use a FileStorage ID to get an existing FileStorage record
     *
     * @param[id] The unique id of the storage element
     * @return a FileStorage object detailing the location of the storage
     */
    fun get(id: String) : FileStorage

    /**
     * In order to read/write this image, callers need a signed URL.
     * @param[id] The unique id of the storage element
     * @param[method] The http method (put to write, get to read)
     * @return a FileStorage object detailing the location of the storage
     */
    fun getSignedUrl(id: String, method: HttpMethod) : String

}

class GcsFileStorageService constructor(credsFile: Path?=null, bucketOverride: String?=null) : FileStorageService {

    @Autowired
    lateinit var properties: ApplicationProperties

    private val gcs: Storage
    private val bucket: String

    init {

        bucket = if (bucketOverride != null) {
            bucketOverride
        } else {
            val project = System.getenv("GCP_PROJECT") ?: throw IllegalStateException("GCP_PROJECT env var not set.")
            "${project}_zorroa_data"
        }

        gcs = if (credsFile!= null && Files.exists(credsFile)) {
            StorageOptions.newBuilder().setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(credsFile.toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }
    }

    override fun create(spec: FileStorageSpec): FileStorage {
        val uri = buildUri(spec)
        val id = buildId(spec)
        return FileStorage(id, uri,"gs", tika.detect(uri),-1, false)
    }

    override fun get(spec: FileStorageSpec): FileStorage {
        val uri = buildUri(spec)
        val id = buildId(spec)
        return FileStorage(id, uri,"gs", tika.detect(uri),-1, false)
    }

    override fun get(id: String): FileStorage {
        return FileStorage(unslashed(id), buildUri(id), "gs", tika.detect(id), -1, false)
    }

    override fun getSignedUrl(id: String, method: HttpMethod) : String {
        var uri = URI(buildUri(id))
        val blob =  gcs.get(bucket, uri.path)
        return blob.signUrl(10, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(method)).toString()
    }

    fun buildUri(id: String): String {
        val org = getOrgId()
        val slashed = slashed(id)
        return "gs://$bucket/orgs/$org/ofs/$slashed"
    }

    fun buildUri(spec: FileStorageSpec) : String {
        val org = getOrgId()
        val variant = spec.variants?.joinToString("_") ?: ""
        val assetId = spec.assetId ?: uuid3.generate(spec.name)
        return "gs://$bucket/orgs/$org/ofs/${spec.category}/$assetId/${spec.name}$variant.${spec.type}"
    }

    fun buildId(spec: FileStorageSpec) : String {
        val variant = spec.variants?.joinToString("_") ?: ""
        val assetId = spec.assetId ?: uuid3.generate(spec.name)
        return "${spec.category}___${assetId}___${spec.name}$variant.${spec.type}"
    }
}

class OfsFileStorageService @Autowired constructor(
        private val ofs: ObjectFileSystem): FileStorageService {

    override fun create(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.prepare(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(spec: FileStorageSpec) : FileStorage {
        val ofile = ofs.get(spec.category, spec.name, spec.type, spec.variants)
        return buildFileStorage(ofile)
    }

    override fun get(id: String) : FileStorage {
        val ofile = ofs.get(slashed(id))
        return buildFileStorage(ofile)
    }

    override fun getSignedUrl(id: String, method: HttpMethod) : String  {
        val ofile = ofs.get(slashed(id))
        return ofile.file.toURI().toString()
    }

    private fun buildFileStorage(ofile: OfsFile) : FileStorage {

        val size : Long = try {
            Files.size(ofile.file.toPath())
        }
        catch (e: Exception) {
            -1
        }

        return FileStorage(
                unslashed(ofile.id),
                ofile.file.toURI().toString(),
                "file",
                tika.detect(ofile.file.toString()),
                size,
                size != -1L)
    }
}

/**
 * Utility method for replacing slashes with triple ___ in proxy IDs
 */
private inline fun unslashed(id: String) : String {
    return id.replace("/", "___")
}

/**
 * Utility method for replacing ___ with triple / in proxy IDs
 */
private inline fun slashed(id: String) : String {
    return id.replace("___", "/")
}


