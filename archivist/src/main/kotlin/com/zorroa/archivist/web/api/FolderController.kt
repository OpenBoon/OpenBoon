package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.Folder
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.SetPermissions
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.service.SearchService
import com.zorroa.sdk.search.AssetSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@RestController
class FolderController @Autowired constructor(
        private val folderService: FolderService,
        private val searchService: SearchService
) {

    val all: List<Folder>
        @GetMapping(value = "/api/v1/folders")
        get() = folderService.getAll()

    @PostMapping(value = "/api/v1/folders")
    fun create(@RequestBody spec: FolderSpec): Folder {
        return folderService.create(spec, false)
    }

    @GetMapping(value = "/api/v1/folders/{id:\\d+}")
    operator fun get(@PathVariable id: Int): Folder {
        return folderService.get(id)
    }

    @GetMapping(value = "/api/v1/folders/{id}/_assetCount")
    fun countAssets(@PathVariable id: Int): Any {
        return HttpUtils.count(searchService.count(folderService.get(id)))
    }

    class FolderAssetCountsRequest {
        var ids: List<Int>? = null
        var search: AssetSearch? = null
    }

    @PostMapping(value = "/api/v1/folders/_assetCounts")
    fun countAssets(@RequestBody req: FolderAssetCountsRequest): Any {
        return HttpUtils.counts(searchService.count(req.ids!!, req.search))
    }

    @GetMapping(value = "/api/v1/folders/_path/**")
    fun get(request: HttpServletRequest): Folder? {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.substring(path.indexOf("/_path/") + 6).replace("//", "/")
        return folderService.get(path)
    }

    @Deprecated("")
    @GetMapping(value = "/api/v1/folders/_exists/**")
    fun exists(request: HttpServletRequest): Any {
        var path = request.getAttribute(
                HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        path = path.replace("/api/v1/folders/_exists", "")
        return HttpUtils.status("folders", path, "exists", folderService.exists(path))
    }

    @PutMapping(value = "/api/v1/folders/{id}")
    fun update(@RequestBody folder: Folder, @PathVariable id: Int): Folder {
        folderService.update(id, folder)
        return folderService.get(folder.id)
    }

    @DeleteMapping(value = "/api/v1/folders/{id}")
    fun delete(@PathVariable id: Int): Any {
        val folder = folderService.get(id)
        return HttpUtils.deleted("folders", id, folderService.trash(folder).count > 0)
    }

    @GetMapping(value = "/api/v1/folders/{id}/folders")
    fun getChildFolders(@PathVariable id: Int): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @Deprecated("")
    @GetMapping(value = "/api/v1/folders/{id}/_children")
    fun getChildren(@PathVariable id: Int): List<Folder> {
        val folder = folderService.get(id)
        return folderService.getChildren(folder)
    }

    @GetMapping(value = "/api/v1/folders/{id}/folders/{name}")
    fun getChild(@PathVariable id: Int, @PathVariable name: String): Folder {
        return folderService.get(id, name)
    }

    @PutMapping(value = "/api/v1/folders/{id}/_permissions")
    @Throws(Exception::class)
    fun setPermissions(@PathVariable id: Int, @RequestBody req: SetPermissions): Any {
        val folder = folderService.get(id)
        if (req.replace) {
            folderService.setAcl(folder, req.acl!!, false, false)
        } else {
            folderService.updateAcl(folder, req.acl!!)
        }
        return folderService.get(folder.id)
    }

    /**
     * Remove the given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @DeleteMapping(value = "/api/v1/folders/{id}/assets")
    @Throws(Exception::class)
    fun removeAssets(
            @RequestBody assetIds: List<String>,
            @PathVariable id: Int?): Any {
        val folder = folderService.get(id!!)
        return folderService.removeAssets(folder, assetIds)
    }

    /**
     * Add a given list of asset Ids from a folder.
     *
     * @param assetIds
     * @param id
     * @throws Exception
     */
    @PostMapping(value = "/api/v1/folders/{id}/assets")
    @Throws(Exception::class)
    fun addAssets(
            @RequestBody assetIds: List<String>,
            @PathVariable id: Int?): Any {
        val folder = folderService.get(id!!)
        return folderService.addAssets(folder, assetIds)
    }
}
