package com.zorroa.analyst.controller

import com.zorroa.common.util.getPublicUrl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun getRoot() : Any {
        return mapOf("endpoint" to getPublicUrl())
    }
}
