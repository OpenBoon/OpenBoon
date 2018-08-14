package com.zorroa.analyst.controller

import com.zorroa.analyst.service.PipelineService
import com.zorroa.common.domain.Pipeline
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PipelineController @Autowired constructor(
        val pipelineService: PipelineService
){

    @GetMapping("/api/v1/pipelines/{id}")
    fun get(@PathVariable id: String) : Pipeline {
        // handles UUID and string at lower level
        return pipelineService.get(id)
    }
}
