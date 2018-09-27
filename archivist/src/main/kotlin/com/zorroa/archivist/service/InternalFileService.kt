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
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.io.FileInputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse

private val tika = Tika()
private val uuid3 = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

private inline fun unslashed(id: String) : String {
    return id.replace("/", "___")
}

private inline fun slashed(id: String) : String {
    return id.replace("___", "/")
}


interface InternalFileStorageService {
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

    fun getReponseEntity(id: String) : ResponseEntity<InputStreamResource>

    fun copyTo(id: String, response: HttpServletResponse)

    fun getSignedUrl(id: String, method: HttpMethod) : String

}

class GcsFileStorageService constructor(credsFile: Path?=null, bucketOverride: String?=null) : InternalFileStorageService {

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

    override fun getReponseEntity(id: String): ResponseEntity<InputStreamResource> {
        var uri = URI(buildUri(id))
        val blob =  gcs.get(bucket, uri.path)
        if (blob != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(blob.contentType))
                    .contentLength(blob.size)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(Channels.newInputStream(blob.reader())))
        } else {
            throw ExternalFileReadException("$id not found")
        }
    }

    override fun copyTo(id: String, response: HttpServletResponse) {
        var uri = URI(buildUri(id))
        val blob =  gcs.get(bucket, uri.path)
        if (blob != null) {
            response.setContentLengthLong(blob.size)
            response.contentType = blob.contentType
            Channels.newInputStream(blob.reader()).copyTo(response.outputStream)
        } else {
            throw ExternalFileReadException("$id not found")
        }
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
        private val ofs: ObjectFileSystem): InternalFileStorageService {

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

    override fun getReponseEntity(id: String): ResponseEntity<InputStreamResource> {
        val ofsFile = ofs.get(slashed(id))
        val path = ofsFile.file.toPath()
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tika.detect(path)))
                .contentLength(Files.size(path))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(FileInputStream(path.toFile())))
    }

    override fun copyTo(id: String, response: HttpServletResponse) {
        val ofsFile = ofs.get(slashed(id))
        val path = ofsFile.file.toPath()
        response.setContentLengthLong(Files.size(path))
        response.contentType = tika.detect(path)
        FileInputStream(ofsFile.file).copyTo(response.outputStream)
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


