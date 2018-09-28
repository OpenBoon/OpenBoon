package com.zorroa.archivist.web.api

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class InternalFileController @Autowired constructor(
        private val fileStorageService: FileStorageService) {

    @PostMapping("/api/v1/file-storage")
    fun create(@RequestBody spec: FileStorageSpec) : FileStorage {
        return fileStorageService.create(spec)
    }

    @GetMapping("/api/v1/file-storage/{id}")
    fun get(@PathVariable id: String) : FileStorage {
        return fileStorageService.get(id)
    }

    @RequestMapping("/api/v1/file-storage/_stat", method = [RequestMethod.GET, RequestMethod.POST])
    fun stat(@RequestBody spec: FileStorageSpec) : FileStorage {
        return fileStorageService.get(spec)
    }

    @GetMapping("/api/v1/file-storage/{id}/_download-uri")
    fun getDownloadURI(@PathVariable id: String) : Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.GET))
    }

    @GetMapping("/api/v1/file-storage/{id}/_upload-uri")
    fun getUploadURI(@PathVariable id: String) : Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.POST))
    }
}