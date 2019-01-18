package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.service.PipelineService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.util.Json
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
class PipelineController @Autowired constructor(
        val pipelineService: PipelineService
){

    @GetMapping(value=["/api/v1/pipelines/{id}"])
    fun get(@PathVariable id: String) : Pipeline {
        // handles UUID and string at lower level
        return pipelineService.get(id)
    }

    @PostMapping(value = ["/api/v1/pipelines"])
    fun create(@RequestBody spec: PipelineSpec): Pipeline {
        println("Calling create")
        return pipelineService.create(spec)
    }

    @GetMapping(value = ["/api/v1/pipelines/{id}/_export"], produces = ["application/octet-stream"])
    fun export(@PathVariable id: String, rsp: HttpServletResponse): ByteArray {
        val pipeline: Pipeline = getPipeline(id)
        rsp.setHeader("Content-disposition", "attachment; filename=\"" + pipeline.name + ".json\"")
        return Json.prettyString(pipeline).toByteArray()
    }

    @GetMapping(value = ["/api/v1/pipelines"])
    fun getPaged(@RequestParam(value = "page", required = false) page: Int?,
                 @RequestParam(value = "count", required = false) count: Int?): PagedList<Pipeline> {
        return pipelineService.getAll(Pager(page, count))
    }

    @PutMapping(value = ["/api/v1/pipelines/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody spec: Pipeline): Any {
        return HttpUtils.updated("pipelines", id, pipelineService.update(spec), pipelineService.get(id))
    }

    @DeleteMapping(value = ["/api/v1/pipelines/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        return HttpUtils.deleted("pipelines", id, pipelineService.delete(id))
    }

    fun getPipeline(nameOrId : String) : Pipeline {
        return try {
            pipelineService.get(UUID.fromString(nameOrId))
        } catch (e:IllegalArgumentException) {
            pipelineService.get(nameOrId)
        }
    }
}
