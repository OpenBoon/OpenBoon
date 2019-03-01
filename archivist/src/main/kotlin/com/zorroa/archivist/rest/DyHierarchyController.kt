package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.service.DyHierarchyService
import com.zorroa.archivist.service.FolderService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Created by chambers on 8/10/16.
 */
@RestController
@Timed
class DyHierarchyController @Autowired constructor(
        val folderService : FolderService,
        val dyHierarchyService: DyHierarchyService
){

    @GetMapping(value = ["/api/v1/dyhi/_folder/{id}"])
    fun getByFolder(@PathVariable id: UUID): DyHierarchy {
        val f = folderService.get(id)
        return dyHierarchyService.get(f)
    }

    @PostMapping(value = ["/api/v1/dyhi"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: DyHierarchySpec): DyHierarchy {
        return dyHierarchyService.create(spec)
    }

    @PostMapping(value = ["/api/v1/dyhi/{id}"])
    fun delete(@PathVariable id: UUID): Map<String, Any> {
        val dh = dyHierarchyService.get(id)
        val result = dyHierarchyService.delete(dh)
        return HttpUtils.status("DyHierarchy", id, "delete", result)
    }

    @PostMapping(value = ["/api/v1/dyhi/{id}/_run"])
    @Throws(Exception::class)
    fun run(@PathVariable id: UUID) : Any {
        val dh = dyHierarchyService.get(id)
        return HttpUtils.updated("DyHierarchy", "run", dyHierarchyService.generate(dh) > 0)
    }

    @GetMapping(value = ["/api/v1/dyhi/{id}"])
    operator fun get(@PathVariable id: UUID): DyHierarchy {
        return dyHierarchyService.get(id)
    }
}
