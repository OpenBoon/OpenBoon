package com.zorroa.analyst.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @GetMapping("/")
    fun getRoot() : String {
        return "Welcome!"
    }
}
