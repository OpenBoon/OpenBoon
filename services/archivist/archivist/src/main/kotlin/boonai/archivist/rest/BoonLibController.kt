package boonai.archivist.rest

import boonai.archivist.domain.BoonLib
import boonai.archivist.domain.BoonLibImportResponse
import boonai.archivist.domain.BoonLibSpec
import boonai.archivist.domain.ProjectToBoonLibCopyRequest
import boonai.archivist.domain.BoonLibUpdateSpec
import boonai.archivist.domain.BoonLibFilter
import boonai.archivist.service.BoonLibService
import boonai.archivist.storage.BoonLibStorageService
import boonai.archivist.storage.ProjectStorageService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiParam
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.PutMapping
import java.util.UUID
import javax.servlet.http.HttpServletRequest

@RestController
class BoonLibController(
    val projectStorageService: ProjectStorageService,
    val boonLibStorageService: BoonLibStorageService,
    val boonLibService: BoonLibService
) {

    @PreAuthorize("hasAuthority('SystemManage')")
    @PostMapping(value = ["/api/v3/boonlibs"])
    @ResponseBody
    fun create(@RequestBody spec: BoonLibSpec): BoonLib {
        return boonLibService.createBoonLib(spec)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v3/boonlibs/{id}"])
    @ResponseBody
    fun update(@PathVariable id: UUID, @RequestBody spec: BoonLibUpdateSpec): BoonLib {
        val boonLib = boonLibService.getBoonLib(id)
        boonLibService.updateBoonLib(boonLib, spec)
        return boonLib
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/boonlibs/{id}"])
    @ResponseBody
    fun get(@PathVariable id: UUID): BoonLib {
        return boonLibService.getBoonLib(id)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/boonlibs/_findOne"])
    @ResponseBody
    fun findOne(@ApiParam("Find One.") @RequestBody filter: BoonLibFilter): BoonLib {
        return boonLibService.findOneBoonLib(filter)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/boonlibs/_search"])
    @ResponseBody
    fun search(
        @ApiParam("Search filter.") @RequestBody filter: BoonLibFilter,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): Any {
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return boonLibService.findAll(filter)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping(value = ["/api/v3/boonlibs/{id}/_import"])
    @ResponseBody
    fun importBoonLib(@PathVariable id: UUID): BoonLibImportResponse {
        val lib = boonLibService.getBoonLib(id)
        return boonLibService.importBoonLib(lib)
    }

    /**
     * Endpoints below are used by job system.
     */

    /**
     * Handle uploading a file into BoonLib.
     */
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/boonlibs/_upload/{libId}/{itemId}/{name}"])
    @ResponseBody
    fun uploadFile(
        @PathVariable libId: String,
        @PathVariable itemId: String,
        @PathVariable name: String,
        @RequestHeader("Content-Length") size: String,
        req: HttpServletRequest
    ): Any {
        val path = "boonlib/$libId/$itemId/$name"
        boonLibStorageService.store(path, size.toLong(), req.inputStream)
        return HttpUtils.status("BoobLib", "upload", true)
    }

    /**
     * Copy files from ProjectStorage into BoonLibs
     */
    @PreAuthorize("hasAnyAuthority('SystemProjectDecrypt','SystemManage')")
    @PostMapping(value = ["/api/v3/boonlibs/_copy_from_project"])
    @ResponseBody
    fun copyIntoBoonLib(
        @RequestBody req: ProjectToBoonLibCopyRequest
    ): Any {
        boonLibStorageService.copyFromProject(req.paths)
        return HttpUtils.status("BoonLib", "copy", true)
    }
}
