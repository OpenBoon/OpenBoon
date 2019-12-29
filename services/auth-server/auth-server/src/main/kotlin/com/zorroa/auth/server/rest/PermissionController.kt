package com.zorroa.auth.server.rest

import com.zorroa.auth.client.Permission
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class PermissionController {

    @ApiOperation("Return a list of all permissions and their use.")
    @RequestMapping("/auth/v1/permissions", method = [RequestMethod.GET])
    fun getAll(): List<Map<String, Any>> {
        return Permission.values().map {
            mapOf(
                "name" to it.name,
                "description" to it.description
            )
        }
    }
}