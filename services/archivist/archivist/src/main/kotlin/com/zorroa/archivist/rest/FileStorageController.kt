package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.getFileLocator
import com.zorroa.archivist.storage.ProjectStorageService
import io.swagger.annotations.ApiOperation
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FileStorageController(
    val projectStorageService: ProjectStorageService
) {

    @ApiOperation("Stream a file associated with any entity.")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/files/_stream/{entityType}/{entityId}/{category}/{name}"])
    @ResponseBody
    fun streamFile(
        @PathVariable entityType: String,
        @PathVariable entityId: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): ResponseEntity<Resource> {
        val locator = getFileLocator(entityType, entityId, category, name)
        return projectStorageService.stream(locator)
    }

    @ApiOperation("Get get underlying file location.", hidden = true)
    // Only job runners can get this.
    @PreAuthorize("hasAuthority('SystemProjectDecrypt')")
    @GetMapping(value = ["/api/v3/files/_locate/{entityType}/{entityId}/{category}/{name}"])
    @ResponseBody
    fun getCloudLocation(
        @PathVariable entityType: String,
        @PathVariable entityId: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): Any {
        val locator = getFileLocator(entityType, entityId, category, name)
        return mapOf("uri" to projectStorageService.getNativeUri(locator))
    }
}
