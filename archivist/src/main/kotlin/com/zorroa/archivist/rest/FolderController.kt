package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.BatchUpdateAssetLinks
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.FolderUpdate
import com.zorroa.archivist.domain.SetPermissions
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.SearchService
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.aspectj.weaver.tools.cache.SimpleCacheFactory.path
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.HandlerMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
@Timed
@Api(tags = ["Folder"], description = "Operations for interacting with Folders.")
class FolderController @Autowired constructor(
    private val folderService: FolderService,
    private val searchService: SearchService
) {

    val all: List<Folder>
        @GetMapping(value = ["/api/v1/folders"])
        get() = folderService.getAll()

    @ApiOperation("Create a Folder.")
    @PostMapping(value = ["/api/v1/folders"])
    @Throws(Exception::class)
    fun create(@ApiParam("Folder to create.") @RequestBody spec: FolderSpec): Folder {
        return folderService.create(spec, false)
    }

    @ApiOperation("Get a Folder.")
    @GetMapping(value = ["/api/v1/folders/{id:[a-fA-F0-9\\-]{36}}"])
    operator fun get(@ApiParam("UUID of the Folder.") @PathVariable id: UUID): Folder {
        return folderService.get(id)
    }

    @ApiOperation("Get the number of Assets in a Folder.")
    @GetMapping(value = ["/api/v1/folders/{id}/_assetCount"])
    fun countAssets(@ApiParam("UUID of the Folder.") @PathVariable id: UUID): Any {
        return HttpUtils.count(searchService.count(folderService.get(id)))
    }

    @ApiModel("Folder Asset Counts Request", description = "Request to get the number of Assets in some Folders.")
    class FolderAssetCountsRequest {
        @ApiModelProperty("UUIDs of Folder to return counts for.") var ids: List<UUID>? = null
        @ApiModelProperty("Search filter to apply to the contents of the Folders.") var search: AssetSearch? = null
    }

    @ApiOperation("Get the number of assets in a list of Folders that match a search filter.")
    @PostMapping(value = ["/api/v1/folders/_assetCounts"])
    fun countAssets(@RequestBody req: FolderAssetCountsRequest): Any {
        return HttpUtils.counts(searchService.count(req.ids!!, req.search))
    }

    @ApiOperation("Get a Folder from its path.",
        notes = "The path is taken from the url. Everything after '/_path' is used as the path to locate the folder.")
    @GetMapping(value = ["/api/v1/folders/_path/**"])
    fun get(request: HttpServletRequest): Folder? {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.substring(path.indexOf("/_path/") + 6).replace("//", "/")
        return folderService.get(path)
    }

    @ApiOperation("Get a Folder from its path.")
    @GetMapping(value = ["/api/v2/folders/_getByPath"])
    fun getByPathV2(
        @ApiParam("Path to get the Folder from. Example: {\"path\": \"/Users/my/folder\"}")
            @RequestBody req: Map<String, String>
    ): Folder? {
        return folderService.get(req.getValue("path"))
    }

    @ApiOperation("Determine if a folder exists.")
    @GetMapping(value = ["/api/v2/folders/_existsByPath"])
    fun existsV2(
        @ApiParam("Path to the Folder. Example: {\"path\":\"/Users/my/folder\"}")
            @RequestBody req: Map<String, String>
    ): Any {
        return HttpUtils.status("folders", path, "exists",
                folderService.exists(req.getValue("path")))
    }

    @ApiOperation("Get the root folder.",
        notes = "There is always one root folder and it can be used to traverse the folder tree.")
    @GetMapping(value = ["/api/v1/folders/_root"])
    fun getRootFolder(): Any {
        return folderService.getRoot()
    }

    @Deprecated("")
    @ApiOperation("DEPRECATED: Do not use.", hidden = true)
    @GetMapping(value = ["/api/v1/folders/_exists/**"])
    fun exists(request: HttpServletRequest): Any {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.replace("/api/v1/folders/_exists", "")
        return HttpUtils.status("folders", path, "exists", folderService.exists(path))
    }

    @ApiOperation("Update a Folder.")
    @PutMapping(value = ["/api/v1/folders/{id}"])
    fun update(
        @ApiParam("Folder update.") @RequestBody folder: FolderUpdate,
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID
    ): Folder {
        folderService.update(id, folder)
        return folderService.get(id)
    }

    @ApiOperation("Delete a Folder.")
    @DeleteMapping(value = ["/api/v1/folders/{id}"])
    fun delete(@ApiParam("UUID of the Folder.") @PathVariable id: UUID): Any {
        val folder = folderService.get(id)
        return HttpUtils.deleted("folders", id, folderService.trash(folder).count > 0)
    }

    @ApiOperation("Get a Folder's children Folders.",
        notes = "This can be used to traverse the folder tree. Often used in conjunction with /api/v1/folders/_root.")
    @GetMapping(value = ["/api/v1/folders/{id}/folders"])
    fun getChildFolders(@ApiParam("UUID of the Folder.") @PathVariable id: UUID): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @Deprecated("")
    @ApiOperation("DEPRECATED: Do not use.", hidden = true)
    @GetMapping(value = ["/api/v1/folders/{id}/_children"])
    fun getChildren(@ApiParam("UUID of the Folder.") @PathVariable id: UUID): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @ApiOperation("Get a child Folder by name.")
    @GetMapping(value = ["/api/v1/folders/{id}/folders/{name}"])
    fun getChild(
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID,
        @ApiParam("Name of the child Folder.") @PathVariable name: String
    ): Folder {
        return folderService.get(id, name)
    }

    @ApiOperation("DEPRECATED: Do not use.", hidden = true)
    @Deprecated("use _acl")
    @PutMapping(value = ["/api/v1/folders/{id}/_permissions"])
    @Throws(Exception::class)
    fun setPermissions(@ApiParam("UUID of the Folder.") @PathVariable id: UUID, @RequestBody req: SetPermissions): Any {
        val folder = folderService.get(id)
        if (req.replace) {
            folderService.setAcl(folder, req.acl!!, false, false)
        } else {
            folderService.updateAcl(folder, req.acl!!)
        }
        return folderService.get(folder.id)
    }

    @ApiOperation("Update the access control list (ACL) for a Folder.")
    @PutMapping(value = ["/api/v1/folders/{id}/_acl"])
    @Throws(Exception::class)
    fun setAcl(
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID,
        @ApiParam("New ACL.") @RequestBody req: SetPermissions
    ): Any {
        val folder = folderService.get(id)
        if (req.replace) {
            folderService.setAcl(folder, req.acl, false, false)
        } else {
            folderService.updateAcl(folder, req.acl)
        }
        return folderService.get(folder.id)
    }

    @ApiOperation("Remove Assets from a Folder.",
        notes = "The Assets are removed from the Folder but not deleted.")
    @DeleteMapping(value = ["/api/v1/folders/{id}/assets"])
    @Throws(Exception::class)
    fun removeAssets(
        @ApiParam("List of Asset UUIDs to remove.") @RequestBody assetIds: List<String>,
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID
    ): Any {
        val folder = folderService.get(id)
        return folderService.removeAssets(folder, assetIds)
    }

    @ApiOperation("Add Assets to a Folder.")
    @PutMapping(value = ["/api/v2/folders/{id}/assets"])
    @Throws(Exception::class)
    fun addAssets(
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID,
        @ApiParam("Filter to get Assets to add to the Folder.") @RequestBody req: BatchUpdateAssetLinks
    ): Any {
        val folder = folderService.get(id)
        val result = folderService.addAssets(folder, req)
        return mapOf("successCount" to result.updatedAssetIds.size, "erroredAssetIds" to result.erroredAssetIds)
    }

    @ApiOperation("Add Assets to a Folder.")
    @PostMapping(value = ["/api/v1/folders/{id}/assets"])
    @Throws(Exception::class)
    fun addAssets(
        @ApiParam("List of Asset UUIDs to add to a Folder.") @RequestBody assetIds: List<String>,
        @ApiParam("UUID of the Folder.") @PathVariable id: UUID
    ): Any {
        val folder = folderService.get(id)
        val req = BatchUpdateAssetLinks(assetIds, null, null)
        val result = folderService.addAssets(folder, req)
        return mapOf("success" to result.updatedAssetIds, "missing" to result.erroredAssetIds)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FolderController::class.java)
    }
}
