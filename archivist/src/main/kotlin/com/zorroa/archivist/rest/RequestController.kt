package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Request
import com.zorroa.archivist.domain.RequestSpec
import com.zorroa.archivist.service.RequestService
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Timed
@Api(tags = ["Request"], description = "Operations for interacting with Requests.")
class RequestController @Autowired constructor(
    val requestService: RequestService
) {

    @ApiOperation("Create a Request.")
    @PostMapping(value = ["/api/v1/requests"])
    fun create(@ApiParam("Request to create.") @RequestBody spec: RequestSpec): Request {
        return requestService.create(spec)
    }

    @ApiOperation("Get a Request.")
    @GetMapping(value = ["/api/v1/requests/{id}"])
    fun get(@ApiParam("UUID of the Request.") @PathVariable id: UUID): Request {
        return requestService.get(id)
    }
}
