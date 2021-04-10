package boonai.archivist.rest

import boonai.archivist.domain.Pipeline
import boonai.archivist.domain.PipelineFilter
import boonai.archivist.domain.PipelineSpec
import boonai.archivist.domain.PipelineUpdate
import boonai.archivist.domain.ResolvedPipeline
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.PipelineResolverService
import boonai.archivist.service.PipelineService
import boonai.archivist.util.HttpUtils
import boonai.common.util.Json
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
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
import javax.servlet.http.HttpServletResponse

@PreAuthorize("hasAuthority('ProjectManage')")
@RestController
@Api(tags = ["Pipeline"], description = "Operations for interacting with Pipelines.")
class PipelineController @Autowired constructor(
    val pipelineService: PipelineService,
    val pipeineResolverService: PipelineResolverService
) {

    @ApiOperation("Get a Pipeline.")
    @GetMapping(value = ["/api/v1/pipelines/{id}"])
    fun get(@ApiParam("UUID of the Pipeline.") @PathVariable id: UUID): Pipeline {
        return pipelineService.get(id)
    }

    @ApiOperation("Create a Pipeline.")
    @PostMapping(value = ["/api/v1/pipelines"])
    fun create(@ApiParam("Pipeline to create.") @RequestBody spec: PipelineSpec): Pipeline {
        return pipelineService.create(spec)
    }

    @ApiOperation("Returns a Pipeline as a json file.")
    @GetMapping(value = ["/api/v1/pipelines/{id}/_export"], produces = ["application/octet-stream"])
    fun export(@ApiParam("UUID of the Pipeline.") @PathVariable id: UUID, rsp: HttpServletResponse): ByteArray {
        val pipeline: Pipeline = pipelineService.get(id)
        rsp.setHeader("Content-disposition", "attachment; filename=\"" + pipeline.name + ".json\"")
        return Json.prettyString(pipeline).toByteArray()
    }

    @ApiOperation(
        "Get all Pipelines.",
        notes = "Results are paginated."
    )

    @RequestMapping(value = ["/api/v1/pipelines/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    fun getAll(@RequestBody(required = false) filter: PipelineFilter?): KPagedList<Pipeline> {
        return pipelineService.getAll(filter ?: PipelineFilter())
    }

    @RequestMapping(value = ["/api/v1/pipelines/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    fun findOne(@RequestBody filter: PipelineFilter): Pipeline {
        return pipelineService.findOne(filter)
    }

    @ApiOperation("Update a Pipeline.")
    @PutMapping(value = ["/api/v1/pipelines/{id}"])
    fun update(
        @ApiParam("UUID of the Pipeline.") @PathVariable id: UUID,
        @ApiParam("Updated Pipeline.") @RequestBody spec: PipelineUpdate
    ): Any {
        return HttpUtils.updated("pipelines", id, pipelineService.update(id, spec))
    }

    @ApiOperation("Delete a Pipeline.")
    @DeleteMapping(value = ["/api/v1/pipelines/{id}"])
    fun delete(@ApiParam("UUID of the Pipeline.") @PathVariable id: UUID): Any {
        return HttpUtils.deleted("pipelines", id, pipelineService.delete(id))
    }

    @ApiOperation("Resolve a Pipeline to its list of processors.")
    @GetMapping(value = ["/api/v1/pipelines/{id}/_resolve"])
    fun resolve(@ApiParam("UUID of the Pipeline.") @PathVariable id: UUID): ResolvedPipeline {
        return pipeineResolverService.resolve(id)
    }

    @ApiOperation("Resolve a Pipeline to its list of processors.")
    @PostMapping(value = ["/api/v1/pipelines/_resolve_modular"])
    fun resolveStandard(
        @ApiParam("A List of module names") @RequestBody(required = false)
        modules: List<String>?
    ): ResolvedPipeline {
        return pipeineResolverService.resolveModular(modules ?: listOf())
    }
}
