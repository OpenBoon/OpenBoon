package com.zorroa.auth.rest

import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController {

    @RequestMapping("/auth/v1/auth-token", method = [RequestMethod.GET, RequestMethod.POST])
    fun authToken(@RequestHeader headers: HttpHeaders, auth: Authentication): Map<String, Any> {
        return mapOf(
                "projectId" to auth.principal,
                "keyId" to auth.credentials,
                "permissions" to auth.authorities)
    }
}