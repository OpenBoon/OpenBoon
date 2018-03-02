package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineSpecV
import com.zorroa.archivist.service.PipelineService
import com.zorroa.archivist.web.InvalidObjectException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import com.zorroa.sdk.util.Json
import com.zorroa.sdk.util.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
class PipelineController @Autowired constructor(
        private val pipelineService: PipelineService
){

    @PostMapping(value = ["/api/v1/pipelines"])
    fun create(@Valid @RequestBody spec: PipelineSpecV, valid: BindingResult): Pipeline {
        if (valid.hasErrors()) {
            throw InvalidObjectException("Failed to create pipeline", valid)
        }
        return pipelineService.create(spec)
    }

    @GetMapping(value = ["/api/v1/pipelines/{id}"])
    operator fun get(@PathVariable id: String): Pipeline {
        return if (StringUtils.isNumeric(id)) {
            pipelineService.get(Integer.parseInt(id))
        } else {
            pipelineService.get(id)
        }
    }

    @GetMapping(value = ["/api/v1/pipelines/{id}/_export"], produces = ["application/octet-stream"])
    fun export(@PathVariable id: String, rsp: HttpServletResponse): ByteArray {

        val export: Pipeline = if (StringUtils.isNumeric(id)) {
            pipelineService.get(Integer.parseInt(id))
        } else {
            pipelineService.get(id)
        }
        export.id = null
        rsp.setHeader("Content-disposition", "attachment; filename=\"" + export.name + ".json\"")
        return Json.prettyString(export).toByteArray()
    }

    @GetMapping(value = ["/api/v1/pipelines"])
    fun getPaged(@RequestParam(value = "page", required = false) page: Int?,
                 @RequestParam(value = "count", required = false) count: Int?): PagedList<Pipeline> {
        return pipelineService.getAll(Pager(page, count))
    }

    @PutMapping(value = ["/api/v1/pipelines/{id}"])
    fun update(@PathVariable id: Int, @Valid @RequestBody spec: Pipeline, valid: BindingResult): Any {
        checkValid(valid)
        return HttpUtils.updated("pipelines", id, pipelineService.update(id, spec), pipelineService.get(id))
    }

    @DeleteMapping(value = ["/api/v1/pipelines/{id}"])
    fun delete(@PathVariable id: Int): Any {
        val result = pipelineService.delete(id)
        logger.info("pipeline deleted {}", result)
        return HttpUtils.deleted("pipelines", id, result)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(PipelineController::class.java)

        fun checkValid(valid: BindingResult) {
            if (valid.hasErrors()) {
                throw InvalidObjectException("Failed to create pipeline", valid)
            }
        }
    }
}
