package com.zorroa.archivist.web.api

import com.zorroa.common.clients.AnalystClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class ProcessorListController @Autowired constructor(
        val analystClient: AnalystClient
){

    @GetMapping("/api/v1/processor-lists/defaults/{type}")
    fun get(@PathVariable type: String) : Any {
        return analystClient.get("/api/v1/processor-lists/defaults/$type", Map::class.java)
    }
}