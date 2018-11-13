package com.zorroa.archivist.web.api

import com.zorroa.archivist.config.NetworkEnvironment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController @Autowired constructor(val networkEnvironment: NetworkEnvironment){

    @GetMapping("/api/v1/commands")
    fun getCommands() : List<Any> {
        return listOf()
    }
}