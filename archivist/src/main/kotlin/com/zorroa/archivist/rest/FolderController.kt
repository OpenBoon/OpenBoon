package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.SearchService
import org.aspectj.weaver.tools.cache.SimpleCacheFactory.path
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
class FolderController @Autowired constructor(
        private val folderService: FolderService,
        private val searchService: SearchService
) {

    val all: List<Folder>
        @GetMapping(value = ["/api/v1/folders"])
        get() = folderService.getAll()

    @PostMapping(value = ["/api/v1/folders"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: FolderSpec): Folder {
        return folderService.create(spec, false)
    }

    @GetMapping(value = ["/api/v1/folders/{id:[a-fA-F0-9\\-]{36}}"])
    operator fun get(@PathVariable id: UUID): Folder {
        return folderService.get(id)
    }

    @GetMapping(value = ["/api/v1/folders/{id}/_assetCount"])
    fun countAssets(@PathVariable id: UUID): Any {
        return HttpUtils.count(searchService.count(folderService.get(id)))
    }

    class FolderAssetCountsRequest {
        var ids: List<UUID>? = null
        var search: AssetSearch? = null
    }

    @PostMapping(value = ["/api/v1/folders/_assetCounts"])
    fun countAssets(@RequestBody req: FolderAssetCountsRequest): Any {
        return HttpUtils.counts(searchService.count(req.ids!!, req.search))
    }

    @GetMapping(value = ["/api/v1/folders/_path/**"])
    fun get(request: HttpServletRequest): Folder? {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.substring(path.indexOf("/_path/") + 6).replace("//", "/")
        return folderService.get(path)
    }

    @GetMapping(value = ["/api/v2/folders/_getByPath"])
    fun getByPathV2(@RequestBody req: Map<String,String>): Folder? {
        return folderService.get(req.getValue("path"))
    }

    @GetMapping(value = ["/api/v2/folders/_existsByPath"])
    fun existsV2(@RequestBody req: Map<String, String>): Any {
        return HttpUtils.status("folders", path, "exists",
                folderService.exists(req.getValue("path")))
    }

    @GetMapping(value = ["/api/v1/folders/_root"])
    fun getRootFolder(): Any {
        return folderService.getRoot()
    }

    @Deprecated("")
    @GetMapping(value = ["/api/v1/folders/_exists/**"])
    fun exists(request: HttpServletRequest): Any {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.replace("/api/v1/folders/_exists", "")
        return HttpUtils.status("folders", path, "exists", folderService.exists(path))
    }

    @PutMapping(value = ["/api/v1/folders/{id}"])
    fun update(@RequestBody folder: FolderUpdate, @PathVariable id: UUID): Folder {
        folderService.update(id, folder)
        return folderService.get(id)
    }

    @DeleteMapping(value = ["/api/v1/folders/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        val folder = folderService.get(id)
        return HttpUtils.deleted("folders", id, folderService.trash(folder).count > 0)
    }

    @GetMapping(value = ["/api/v1/folders/{id}/folders"])
    fun getChildFolders(@PathVariable id: UUID): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @Deprecated("")
    @GetMapping(value = ["/api/v1/folders/{id}/_children"])
    fun getChildren(@PathVariable id: UUID): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @GetMapping(value = ["/api/v1/folders/{id}/folders/{name}"])
    fun getChild(@PathVariable id: UUID, @PathVariable name: String): Folder {
        return folderService.get(id, name)
    }

    @Deprecated("use _acl")
    @PutMapping(value = ["/api/v1/folders/{id}/_permissions"])
    @Throws(Exception::class)
    fun setPermissions(@PathVariable id: UUID, @RequestBody req: SetPermissions): Any {
        val folder = folderService.get(id)
        if (req.replace) {
            folderService.setAcl(folder, req.acl!!, false, false)
        } else {
            folderService.updateAcl(folder, req.acl!!)
        }
        return folderService.get(folder.id)
    }

    @PutMapping(value = ["/api/v1/folders/{id}/_acl"])
    @Throws(Exception::class)
    fun setAcl(@PathVariable id: UUID, @RequestBody req: SetPermissions): Any {
        val folder = folderService.get(id)
        if (req.replace) {
            folderService.setAcl(folder, req.acl, false, false)
        } else {
            folderService.updateAcl(folder, req.acl)
        }
        return folderService.get(folder.id)
    }

    /**
     * Remove the given list of assetIds from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @DeleteMapping(value = ["/api/v1/folders/{id}/assets"])
    @Throws(Exception::class)
    fun removeAssets(
            @RequestBody assetIds: List<String>,
            @PathVariable id: UUID): Any {
        val folder = folderService.get(id)
        return folderService.removeAssets(folder, assetIds)
    }

    /**
     * Add a given list of assetIds to a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @PutMapping(value = ["/api/v2/folders/{id}/assets"])
    @Throws(Exception::class)
    fun addAssets(@PathVariable id: UUID, @RequestBody req: BatchUpdateAssetLinks): Any {
        val folder = folderService.get(id)
        return folderService.addAssets(folder, req)
    }

    /**
     * Add a given list of assetIds to a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @PostMapping(value = ["/api/v1/folders/{id}/assets"])
    @Throws(Exception::class)
    fun addAssets(
            @RequestBody assetIds: List<String>,
            @PathVariable id: UUID): Any {
        val folder = folderService.get(id)
        val req = BatchUpdateAssetLinks(assetIds, null, null)
        val result =  folderService.addAssets(folder, req)
        return mapOf("success" to result.updatedAssetIds, "missing" to result.erroredAssetIds)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FolderController::class.java)
    }
}
