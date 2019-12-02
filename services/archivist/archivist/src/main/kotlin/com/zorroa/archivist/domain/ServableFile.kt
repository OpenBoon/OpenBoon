package com.zorroa.archivist.domain

import com.zorroa.archivist.service.FileServerService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.nio.file.Path
import javax.servlet.http.HttpServletResponse


/**
 * On object that can be stored somewhere locallly or in the vast
 * reaches of the interweb.
 */

class ServableFile(
    private val fileServerService: FileServerService,
    val uri: URI
) {

    fun exists(): Boolean {
        return fileServerService.objectExists(uri)
    }

    fun isLocal(): Boolean {
        return fileServerService.storedLocally
    }

    fun getSignedUrl(): URL {
        return fileServerService.getSignedUrl(uri)
    }

    fun getReponseEntity(): ResponseEntity<InputStreamResource> {
        return fileServerService.getReponseEntity(uri)
    }

    fun copyTo(response: HttpServletResponse) {
        return fileServerService.copyTo(uri, response)
    }

    fun getLocalFile(): Path? {
        return fileServerService.getLocalPath(uri)
    }

    /**
     * Return an open InputStream for the given file.
     */
    fun getInputStream(): InputStream {
        return fileServerService.getInputStream(uri)
    }

    fun getStat(): FileStat {
        return fileServerService.getStat(uri)
    }

    fun delete(): Boolean {
        return fileServerService.delete(uri)
    }
}