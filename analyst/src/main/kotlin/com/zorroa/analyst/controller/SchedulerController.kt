package com.zorroa.analyst.controller

import com.zorroa.analyst.service.SchedulerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController


@RestController
class SchedulerController @Autowired constructor(
        val schedulerService: SchedulerService) {

    @RequestMapping("/api/v1/scheduler/_pause", method=[RequestMethod.GET, RequestMethod.POST])
    fun pause() : Any {
        return RestResponse(HttpMethod.POST, "/api/v1/scheduler/_pause", schedulerService.pause())
    }

    @RequestMapping("/api/v1/scheduler/_resume", method=[RequestMethod.GET, RequestMethod.POST])
    fun resume() : Any {
        return RestResponse(HttpMethod.POST, "/api/v1/scheduler/_resume", schedulerService.resume())
    }
}
