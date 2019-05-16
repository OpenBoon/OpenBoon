package com.zorroa.archivist.rest

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.ImageService
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
class FileStorageController @Autowired constructor(
        private val fileStorageService: FileStorageService,
        private val fileServerProvider: FileServerProvider,
        private val imageService: ImageService) {

    @RequestMapping("/api/v1/file-storage", method=[RequestMethod.POST, RequestMethod.GET])
    fun create(@RequestBody spec: FileStorageSpec) : FileStorage {
        return fileStorageService.get(spec)
    }

    @GetMapping("/api/v1/file-storage/{id}")
    fun get(@PathVariable id: String) : FileStorage {
        return fileStorageService.get(id)
    }

    @GetMapping("/api/v1/file-storage/{id}/_stat")
    fun stat(@PathVariable id: String) : FileStat {
        val loc = fileStorageService.get(id)
        return fileServerProvider.getServableFile(loc.uri).getStat()
    }

    @GetMapping("/api/v1/file-storage/{id}/_download-uri")
    fun getDownloadURI(@PathVariable id: String) : Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.GET))
    }

    @GetMapping("/api/v1/file-storage/{id}/_upload-uri")
    fun getUploadURI(@PathVariable id: String) : Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.PUT))
    }

    @GetMapping("/api/v1/file-storage/{id}/_stream")
    fun stream(@PathVariable id: String, rsp: HttpServletResponse) {
        val loc = fileStorageService.get(id)
        // At least try to handle the image watermarks for on-prem
        if (loc.mediaType.startsWith("image")) {
            imageService.serveImage(rsp, loc)
        }
        else {
            fileServerProvider.getServableFile(loc).copyTo(rsp)
        }
    }

    /**
     * A 0.39 compatible endpoint for flipbooks.
     */
    @Deprecated("See the /api/v1/file-storage/{id}/_stream endpoint", ReplaceWith("stream(id, rsp)"))
    @RequestMapping(value = ["/api/v1/ofs/{id}"], method = [RequestMethod.GET])
    fun oldStream(@PathVariable id: String, rsp: HttpServletResponse) {
        stream(id, rsp)
    }
}
