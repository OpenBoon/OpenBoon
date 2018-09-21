package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.FileStorageStat
import com.zorroa.archivist.service.FileStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class FileStorageController @Autowired constructor(
        private val fileStorageService: FileStorageService) {

    @PostMapping("/api/v1/file-storage")
    fun create(@RequestBody spec: FileStorageSpec) : FileStorage {
        return fileStorageService.create(spec)
    }

    @RequestMapping("/api/v1/file-storage/_stat", method = [RequestMethod.GET, RequestMethod.POST])
    fun stat(@RequestBody spec: FileStorageSpec) : FileStorageStat {
        return fileStorageService.getStat(spec)
    }
}