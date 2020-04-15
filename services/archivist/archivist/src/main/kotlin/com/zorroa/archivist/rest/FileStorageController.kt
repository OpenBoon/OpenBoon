package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.ProjectFileLocator
import com.zorroa.archivist.domain.ProjectStorageEntity
import com.zorroa.archivist.domain.ProjectStorageRequest
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.DataSetService
import com.zorroa.archivist.storage.ProjectStorageService
import io.swagger.annotations.ApiOperation
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
class FileStorageController(
    val projectStorageService: ProjectStorageService,
    val assetService: AssetService,
    val dataSetService: DataSetService
) {

    @ApiOperation("Store an additional file to an asset.")
    // Only job runner keys can store files.
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/files/_upload"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun uploadFile(
        @RequestPart(value = "file") file: MultipartFile,
        @RequestPart(value = "body") req: ProjectStorageRequest
    ): Any {
        val locator = ProjectFileLocator(req.entity, req.entityId, req.category, req.name)
        validateEntity(locator)

        val spec = ProjectStorageSpec(locator, req.attrs, file.bytes)
        return projectStorageService.store(spec)
    }

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
        val locator = ProjectFileLocator(
            ProjectStorageEntity.find(entityType), entityId, category, name
        )
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
        val locator = ProjectFileLocator(
            ProjectStorageEntity.find(entityType), entityId, category, name
        )
        return mapOf("uri" to projectStorageService.getNativeUri(locator))
    }

    fun validateEntity(locator: ProjectFileLocator) {
        try {
            when (locator.entity) {
                ProjectStorageEntity.ASSET -> {
                    assetService.getAsset(locator.entityId)
                }
                ProjectStorageEntity.DATASET -> {
                    dataSetService.get(UUID.fromString(locator.entityId))
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid ID for type: ${locator.entity}")
        }
    }
}
