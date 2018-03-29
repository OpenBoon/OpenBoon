package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.service.RequestService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class RequestController @Autowired constructor(
        val requestService: RequestService) {

    @PostMapping(value = ["/api/v1/requests"])
    fun create(@RequestBody spec: RequestSpec) : Request {
        return requestService.create(spec)
    }

    @GetMapping(value = ["/api/v1/requests/{id}"])
    fun get(@PathVariable id: UUID): Request {
        return requestService.get(id)
    }
}
