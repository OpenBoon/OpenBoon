package com.zorroa.analyst.controller

import com.zorroa.analyst.service.SchedulerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@RestController
class SchedulerController @Autowired constructor(
        val schedulerService: SchedulerService) {

    @RequestMapping("/api/v1/scheduler/_pause", method=[RequestMethod.GET, RequestMethod.POST])
    fun pause() : Any {
        return mapOf("op" to "pause", "success" to schedulerService.pause())
    }

    @RequestMapping("/api/v1/scheduler/_resume", method=[RequestMethod.GET, RequestMethod.POST])
    fun resume() : Any {
        return mapOf("op" to "resume", "success" to schedulerService.resume())
    }
}
