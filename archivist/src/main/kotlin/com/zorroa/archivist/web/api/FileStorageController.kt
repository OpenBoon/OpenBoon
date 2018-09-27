package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.InternalFileStorageService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
class FileStorageController @Autowired constructor(
        private val internalFileStorageService: InternalFileStorageService) {

    @PostMapping("/api/v1/file-storage")
    fun create(@RequestBody spec: FileStorageSpec) : FileStorage {
        return internalFileStorageService.create(spec)
    }

    @GetMapping("/api/v1/file-storage/**")
    fun get(req: HttpServletRequest) : FileStorage {
        val id = req.requestURL.toString().split("/file-storage/")[1]
        return internalFileStorageService.get(id)

    }
    @RequestMapping("/api/v1/file-storage/_stat", method = [RequestMethod.GET, RequestMethod.POST])
    fun stat(@RequestBody spec: FileStorageSpec) : FileStorage {
        return internalFileStorageService.get(spec)
    }
}