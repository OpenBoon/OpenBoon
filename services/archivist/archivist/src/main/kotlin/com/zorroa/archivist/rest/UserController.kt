package com.zorroa.archivist.rest

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

import java.security.Principal

@RestController
class UserController {

    /**
     * If a user has been specified in the request then the user
     * will have ROLE_USER
     */
    @GetMapping("/api/v1/authed/user")
    @PreAuthorize("hasRole('ROLE_USER')")
    fun user(principal: Principal): ResponseEntity<Principal> {
        return ResponseEntity.ok(principal)
    }

    /**
     * If there is no user in the request but its a valid token, then
     * the role is ROLE_CLIENT
     */
    @GetMapping("/api/v1/authed/client")
    @PreAuthorize("hasRole('ROLE_CLIENT')")
    fun client(principal: Principal): ResponseEntity<Principal> {
        return ResponseEntity.ok(principal)
    }
}