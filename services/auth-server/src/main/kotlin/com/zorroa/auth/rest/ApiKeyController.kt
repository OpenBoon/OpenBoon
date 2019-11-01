package com.zorroa.auth.rest

import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.service.ApiKeyService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.annotation.security.RolesAllowed


@RestController
@PreAuthorize("hasRole('ROLE_SUPERADMIN')")
class ApiKeyController {

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @PostMapping("/auth/v1/apikey")
    fun create(@RequestBody spec: ApiKeySpec): ApiKey {
        return apiKeyService.create(spec)
    }

    @GetMapping("/auth/v1/apikey/{id}")
    fun get(@PathVariable id: UUID): ApiKey {
        return apiKeyService.get(id)
    }

    @DeleteMapping("/auth/v1/apikey/{id}")
    fun delete(@PathVariable id: UUID) {
        apiKeyService.delete(apiKeyService.get(id))
    }

    @GetMapping("/auth/v1/apikey")
    fun findAll() : List<ApiKey> {
        return apiKeyService.findAll()
    }

}
