package com.zorroa.archivist.rest

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.service.FileStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class FileStorageController @Autowired constructor(
        private val fileStorageService: FileStorageService,
        private val fileServerProvider: FileServerProvider) {

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
}