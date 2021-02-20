package boonai.archivist.rest

import boonai.archivist.domain.PipelineMod
import boonai.archivist.domain.PipelineModFilter
import boonai.archivist.domain.PipelineModSpec
import boonai.archivist.domain.PipelineModUpdate
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.PipelineModService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class PipelineModController(val pipelineModService: PipelineModService) {

    @PreAuthorize("hasAuthority('SystemManage')")
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

    @ApiOperation("Search for Pipeline mods")
    @RequestMapping("$URL/_search", method = [RequestMethod.POST, RequestMethod.GET])
    fun search(@RequestBody(required = false) filter: PipelineModFilter?): KPagedList<PipelineMod> {
        return pipelineModService.search(filter ?: PipelineModFilter())
    }

    @ApiOperation("Find a single PipelineMod")
    @RequestMapping("$URL/_find_one", method = [RequestMethod.POST, RequestMethod.GET])
    fun findOne(@RequestBody(required = false) filter: PipelineModFilter?): PipelineMod {
        return pipelineModService.findOne(filter ?: PipelineModFilter())
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @ApiOperation("Update a Pipeline Mod")
    @PutMapping("$URL/{id}")
    fun update(@PathVariable id: UUID, @RequestBody update: PipelineModUpdate): PipelineMod {
        return pipelineModService.update(id, update)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
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
