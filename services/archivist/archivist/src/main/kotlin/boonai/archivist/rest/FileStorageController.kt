package boonai.archivist.rest

import boonai.archivist.domain.ProjectFileLocator
import boonai.archivist.domain.ProjectStorageEntity
import boonai.archivist.domain.ProjectStorageRequest
import boonai.archivist.domain.ProjectStorageSpec
import boonai.archivist.service.AssetService
import boonai.archivist.service.ModelService
import boonai.archivist.storage.BoonLibStorageService
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.FileUtils
import io.swagger.annotations.ApiOperation
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID
import java.util.concurrent.TimeUnit

@RestController
class FileStorageController(
    val projectStorageService: ProjectStorageService,
    val boonLibStorageService: BoonLibStorageService,
    val assetService: AssetService,
    val modelService: ModelService
) {

    @ApiOperation("Store an additional file to an asset.", hidden = true)
    // Only job runner keys can store files.
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/files/_upload"], consumes = ["multipart/form-data"])
    @ResponseBody
    fun uploadFile(
        @RequestPart(value = "file") file: MultipartFile,
        @RequestPart(value = "body") req: ProjectStorageRequest
    ): Any {
        val locator = getValidLocator(req)
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
        // In case this doesn't get replaced.
        val cat = if (category == "__TAG__") {
            "latest"
        } else {
            category
        }

        return if (entityType == "boonlib") {
            boonLibStorageService.stream("boonlib/$entityId/$category/$name")
        } else {
            val locator = getValidLocator(entityType, entityId, cat, name)
            projectStorageService.stream(locator)
        }
    }

    @ApiOperation("Stream a file associated with any entity.")
    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/files/_sign/{entityType}/{entityId}/{category}/{name}"])
    @ResponseBody
    fun signFile(
        @PathVariable entityType: String,
        @PathVariable entityId: String,
        @PathVariable category: String,
        @PathVariable name: String
    ): Map <String, Any> {
        val locator = getValidLocator(entityType, entityId, category, name)
        return projectStorageService.getSignedUrl(locator, false, 60, TimeUnit.MINUTES)
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
        val locator = getValidLocator(entityType, entityId, category, name)
        return mapOf(
            "uri" to projectStorageService.getNativeUri(locator),
            "mediaType" to FileUtils.getMediaType(name)
        )
    }

    @ApiOperation("Sign a storage entity for write.")
    // Only job runner keys can upload files..
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/files/_signed_upload_uri"])
    @ResponseBody
    fun getSignedUploadUri(
        @RequestBody req: ProjectStorageRequest
    ): Any {
        return projectStorageService.getSignedUrl(
            getValidLocator(req), true, 20, TimeUnit.MINUTES
        )
    }

    @ApiOperation("Store an additional file to an asset.")
    // Only job runner keys can store files.
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PutMapping(value = ["/api/v3/files/_attrs"])
    @ResponseBody
    fun setAttrs(@RequestBody req: ProjectStorageRequest): Any {
        val locator = getValidLocator(req)
        return projectStorageService.setAttrs(locator, req.attrs)
    }

    /**
     * Get a [ProjectFileLocator] and validate the naming and existence of
     * associated entity.
     */
    fun getValidLocator(req: ProjectStorageRequest): ProjectFileLocator {
        return validateLocator(req.getLocator())
    }

    /**
     * Get a [ProjectFileLocator] and validate the naming and existence of
     * associated entity.
     */
    fun getValidLocator(entity: String, id: String, category: String, name: String): ProjectFileLocator {
        val locator = ProjectFileLocator(
            ProjectStorageEntity.find(entity), id, category.toLowerCase(), name
        )
        return validateLocator(locator)
    }

    /**
     * Validate a [ProjectFileLocator]. This means the names are proper and
     * the entity it references actually exists.
     */
    fun validateLocator(locator: ProjectFileLocator): ProjectFileLocator {
        if (!REGEX_CATEGORY.matches(locator.category)) {
            throw IllegalArgumentException("The category ${locator.category} must be alpha numeric")
        }

        try {
            when (locator.entity) {
                ProjectStorageEntity.ASSETS -> {
                    assetService.getAsset(locator.entityId)
                }
                ProjectStorageEntity.MODELS -> {
                    modelService.getModel(UUID.fromString(locator.entityId))
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid ID for type: ${locator.entity}")
        }

        return locator
    }

    companion object {

        val REGEX_CATEGORY = Regex("[a-z0-9\\-_]+", RegexOption.IGNORE_CASE)

        val REGEX_NAME = Regex("[a-z0-9\\-_\\.]+", RegexOption.IGNORE_CASE)
    }
}
