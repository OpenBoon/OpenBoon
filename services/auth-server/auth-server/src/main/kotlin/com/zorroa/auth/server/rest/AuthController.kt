package com.zorroa.auth.server.rest

import com.zorroa.zmlp.apikey.ZmlpActor
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@Api(tags = ["Auth"], description = "Auth Operations")
class AuthController {

    @ApiOperation("Authenticate a signed JWT token and return the projectId and permissions.")
    @RequestMapping("/auth/v1/auth-token", method = [RequestMethod.GET, RequestMethod.POST])
    fun authToken(@RequestHeader headers: HttpHeaders, auth: Authentication): Map<String, Any> {
        val user = auth.principal as ZmlpActor
        return mapOf(
            "name" to user.name,
            "projectId" to user.projectId,
            "id" to user.id,
            "permissions" to auth.authorities.map { it.toString() }
        )
    }
}
