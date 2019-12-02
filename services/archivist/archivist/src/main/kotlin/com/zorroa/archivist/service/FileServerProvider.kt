package com.zorroa.archivist.service

import com.google.common.net.UrlEscapers
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ServableFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.net.URI

/**
 * FileServerProvider handles proving a proper FileServerService based on a given URI.
 */
@Service
interface FileServerProvider {

    fun getStorageUri(doc: Asset): URI {
        val stream: String = doc.getAttr("source.path") ?: throw IllegalStateException("${doc.id} has no source.path")
        return if (stream.contains(":/")) {
            URI(UrlEscapers.urlFragmentEscaper().escape(stream))
        } else {
            URI(UrlEscapers.urlFragmentEscaper().escape("file://$stream"))
        }
    }

    fun getServableFile(storage: FileStorage): ServableFile = getServableFile(storage.uri)
    fun getServableFile(uri: URI): ServableFile
    fun getServableFile(doc: Asset): ServableFile
    fun getServableFile(uri: String): ServableFile = getServableFile(URI(uri))
}

@Service
class FileServerProviderImpl(
    private val fileServerService: FileServerService
) : FileServerProvider {

    /*
    private val services: Map<String, FileServerService>


    init {
        services = mutableMapOf()
        val internalStorageType = properties.getString("archivist.storage.type")

        if (internalStorageType == "gcs") {
            services["gcs"] = GcpFileServerService(credentials)
        } else {
            services["local"] = LocalFileServerService()
        }
    }*/

    fun getServerService(uri: URI): FileServerService {
        return fileServerService

        /*val type = when (uri.scheme) {
            "gs" -> "gcs"
            "file" -> "local"
            null -> "local"
            else -> uri.scheme
        }
        return services[type] ?: throw FileServerException(
            "Unable to find storage service for: $type"
        )*/
    }

    override fun getServableFile(uri: URI): ServableFile {
        val service = getServerService(uri)
        return ServableFile(service, uri)
    }

    override fun getServableFile(doc: Asset): ServableFile {
        val uri = getStorageUri(doc)
        return getServableFile(uri)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileServerProviderImpl::class.java)
    }
}