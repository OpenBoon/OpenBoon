package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.service.PipelineService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.util.Json
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(tags = ["Pipeline"], description = "Operations for interacting with Pipelines.")
class PipelineController @Autowired constructor(
    val pipelineService: PipelineService
) {

    @ApiOperation("Get a Pipeline.")
    @GetMapping(value = ["/api/v1/pipelines/{id}"])
    fun get(@ApiParam("UUID of the Pipeline.") @PathVariable id: String): Pipeline {
        // handles UUID and string at lower level
        return pipelineService.get(id)
    }

    @ApiOperation("Create a Pipeline.")
    @PostMapping(value = ["/api/v1/pipelines"])
    fun create(@ApiParam("Pipeline to create.") @RequestBody spec: PipelineSpec): Pipeline {
        return pipelineService.create(spec)
    }

    @ApiOperation("Returns a Pipeline as a json file.")
    @GetMapping(value = ["/api/v1/pipelines/{id}/_export"], produces = ["application/octet-stream"])
    fun export(@ApiParam("UUID of the Pipeline.") @PathVariable id: String, rsp: HttpServletResponse): ByteArray {
        val pipeline: Pipeline = getPipeline(id)
        rsp.setHeader("Content-disposition", "attachment; filename=\"" + pipeline.name + ".json\"")
        return Json.prettyString(pipeline).toByteArray()
    }

    @ApiOperation("Get all Pipelines.",
        notes = "Results are paginated.")
    @GetMapping(value = ["/api/v1/pipelines"])
    fun getPaged(
        @ApiParam("Pages to return.") @RequestParam(value = "page", required = false) page: Int?,
        @ApiParam("Results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): PagedList<Pipeline> {
        return pipelineService.getAll(Pager(page, count))
    }

    @ApiOperation("Update a Pipeline.")
    @PutMapping(value = ["/api/v1/pipelines/{id}"])
    fun update(
        @ApiParam("UUID of the Pipeline.") @PathVariable id: UUID,
        @ApiParam("Updated Pipeline.") @RequestBody spec: Pipeline
    ): Any {
        return HttpUtils.updated("pipelines", id, pipelineService.update(spec), pipelineService.get(id))
    }

    @ApiOperation("Delete a Pipeline.")
    @DeleteMapping(value = ["/api/v1/pipelines/{id}"])
    fun delete(@ApiParam("UUID of the Pipeline.") @PathVariable id: UUID): Any {
        return HttpUtils.deleted("pipelines", id, pipelineService.delete(id))
    }

    fun getPipeline(nameOrId: String): Pipeline {
        return try {
            pipelineService.get(UUID.fromString(nameOrId))
        } catch (e: IllegalArgumentException) {
            pipelineService.get(nameOrId)
        }
    }
}
