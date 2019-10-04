package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.service.DyHierarchyService
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Created by chambers on 8/10/16.
 */
@RestController
@Timed
@Api(tags = ["Dynamic Hierarchy"], description = "Operations for interacting with Dynamic Hierarchies (DyHi).")
class DyHierarchyController @Autowired constructor(
    val folderService: FolderService,
    val dyHierarchyService: DyHierarchyService
) {

    @ApiOperation(value = "Get a DyHi from a folder.",
        notes = "Given the ID of a folder within in a DyHi the DyHi is returned.")
    @GetMapping(value = ["/api/v1/dyhi/_folder/{id}"])
    fun getByFolder(@ApiParam(value = "UUID of the folder.") @PathVariable id: UUID): DyHierarchy {
        val f = folderService.get(id)
        return dyHierarchyService.get(f)
    }

    @ApiOperation(value = "Create a DyHi.")
    @PostMapping(value = ["/api/v1/dyhi"])
    @Throws(Exception::class)
    fun create(@ApiParam(value = "DyHi to create.") @RequestBody spec: DyHierarchySpec): DyHierarchy {
        return dyHierarchyService.create(spec)
    }

    @ApiOperation(value = "Delete a DyHi.")
    @DeleteMapping(value = ["/api/v1/dyhi/{id}"])
    fun delete(@ApiParam(value = "UUID of the DyHi.") @PathVariable id: UUID): Map<String, Any> {
        val dh = dyHierarchyService.get(id)
        val result = dyHierarchyService.delete(dh)
        return HttpUtils.status("DyHierarchy", id, "delete", result)
    }

    @ApiOperation(value = "Run a DyHi.",
        notes = "Initiates the processes that generate the contents of a DyHi.")
    @PostMapping(value = ["/api/v1/dyhi/{id}/_run"])
    @Throws(Exception::class)
    fun run(@ApiParam(value = "UUID of the DyHi.") @PathVariable id: UUID): Any {
        val dh = dyHierarchyService.get(id)
        return HttpUtils.updated("DyHierarchy", "run", dyHierarchyService.generate(dh) > 0)
    }

    @ApiOperation(value = "Get a DyHi.")
    @GetMapping(value = ["/api/v1/dyhi/{id}"])
    operator fun get(@ApiParam(value = "UUID of the DyHi.") @PathVariable id: UUID): DyHierarchy {
        return dyHierarchyService.get(id)
    }
}
