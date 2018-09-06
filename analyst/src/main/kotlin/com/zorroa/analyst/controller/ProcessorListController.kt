package com.zorroa.analyst.controller

import com.zorroa.analyst.service.PipelineService
import com.zorroa.common.domain.PipelineType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProcessorListController @Autowired constructor(
        val pipelineService: PipelineService
){

    @GetMapping("/api/v1/processor-lists/defaults/{type}")
    fun get(@PathVariable type: String) : Any {
        val plType = PipelineType.valueOf(type.capitalize())
        return mapOf("type" to type, "processors" to pipelineService.buildDefaultProcessorList(plType))
    }
}
