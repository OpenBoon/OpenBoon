package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineModUpdate
import com.zorroa.archivist.service.PipelineModService
import com.zorroa.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('SystemManage')")
@RestController
class PipelineModController(val pipelineModService: PipelineModService) {

    @ApiOperation("Create a new Pipeline Mod")
    @PostMapping(URL)
    fun create(@RequestBody spec: PipelineModSpec): PipelineMod {
        return pipelineModService.create(spec)
    }

    @ApiOperation("Get a PipelineMod by Id")
    @GetMapping("$URL/{id}")
    fun get(@PathVariable id: UUID): PipelineMod {
        return pipelineModService.get(id)
    }

    @ApiOperation("Update a Pipeline Mpd")
    @PutMapping("$URL/{id}")
    fun update(@PathVariable id: UUID, @RequestBody update: PipelineModUpdate): PipelineMod {
        return pipelineModService.update(id, update)
    }

    @ApiOperation("Delete a Pipeline Mod, assuming it's not in use.")
    @DeleteMapping("$URL/{id}")
    fun delete(@PathVariable id: UUID): Any {
        pipelineModService.delete(id)
        return HttpUtils.deleted("pipeline-mod", id, true)
    }

    companion object {
        const val URL = "/api/v1/pipeline-mods"
    }
}
