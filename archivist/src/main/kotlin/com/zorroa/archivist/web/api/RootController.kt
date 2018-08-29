package com.zorroa.archivist.web.api

import com.zorroa.common.server.NetworkEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController @Autowired constructor(val networkEnvironment: NetworkEnvironment){

    @GetMapping("/")
    fun getRoot() : Any {
        return mapOf("endpoint" to networkEnvironment.getPublicUrl("zorroa-archivist"))
    }
}