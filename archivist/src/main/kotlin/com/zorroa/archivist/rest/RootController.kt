package com.zorroa.archivist.rest

import com.zorroa.archivist.config.NetworkEnvironment
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
class RootController @Autowired constructor(val networkEnvironment: NetworkEnvironment){

    @GetMapping("/api/v1/commands")
    fun getCommands() : List<Any> {
        return listOf()
    }
}
