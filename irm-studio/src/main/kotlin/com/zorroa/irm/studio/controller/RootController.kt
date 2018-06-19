package com.zorroa.irm.studio.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun getRoot() : Map<String, Any> {
        return mapOf("1+1=" to "3")
    }
}
