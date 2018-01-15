package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.service.DyHierarchyService
import com.zorroa.archivist.service.FolderService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Created by chambers on 8/10/16.
 */
@RestController
class DyHierarchyController @Autowired constructor(
        val folderService : FolderService,
        val dyHierarchyService: DyHierarchyService
){

    @GetMapping(value = ["/api/v1/dyhi/_folder/{id}"])
    fun getByFolder(@PathVariable id: Int): DyHierarchy {
        val f = folderService.get(id)
        return dyHierarchyService.get(f)
    }

    @PostMapping(value = ["/api/v1/dyhi"])
    fun create(@RequestBody spec: DyHierarchySpec): DyHierarchy {
        return dyHierarchyService.create(spec)
    }

    @PostMapping(value = ["/api/v1/dyhi/{id}"])
    fun delete(@PathVariable id: Int): Map<String, Any> {
        val dh = dyHierarchyService.get(id)
        val result = dyHierarchyService.delete(dh)
        return HttpUtils.status("DyHierarchy", id, "delete", result)
    }

    @GetMapping(value = ["/api/v1/dyhi/{id}"])
    operator fun get(@PathVariable id: Int): DyHierarchy {
        return dyHierarchyService.get(id)
    }
}
