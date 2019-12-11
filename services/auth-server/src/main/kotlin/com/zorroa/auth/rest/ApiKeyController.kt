package com.zorroa.auth.rest

import com.zorroa.auth.JSON_MAPPER
import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ApiKeyFilter
import com.zorroa.auth.domain.ApiKeySpec
import com.zorroa.auth.service.ApiKeyService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@PreAuthorize("hasAuthority('SuperAdmin')")
@Api(tags = ["API Key"], description = "Operations for managing API Keys.")
class ApiKeyController {

    @Autowired
    lateinit var apiKeyService: ApiKeyService

    @PostMapping("/auth/v1/apikey")
    @ApiOperation("Create Api Key")
    fun create(@RequestBody spec: ApiKeySpec): ApiKey {
        return apiKeyService.create(spec)
    }

    @GetMapping("/auth/v1/apikey/{id}")
    @ApiOperation("Create Api Key by Id")
    fun get(@PathVariable id: UUID): ApiKey {
        return apiKeyService.get(id)
    }

    @RequestMapping("/auth/v1/apikey/_findOne", method = [RequestMethod.GET, RequestMethod.POST])
    @ApiOperation("Create Unique Api Key by Filtering")
    fun get(@RequestBody(required = false) filter: ApiKeyFilter?): ApiKey {
        return apiKeyService.findOne(filter ?: ApiKeyFilter())
    }

    @GetMapping("/auth/v1/apikey/{id}/_download")
    @ApiOperation("Download API Key")
    fun download(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val key = apiKeyService.get(id)
        val bytes = JSON_MAPPER.writeValueAsBytes(key.getMinimalApiKey())

        val responseHeaders = HttpHeaders()
        responseHeaders.set("charset", "utf-8")
        responseHeaders.contentType = MediaType.valueOf("application/json")
        responseHeaders.contentLength = bytes.size.toLong()
        responseHeaders.set("Content-disposition", "attachment; filename=zorroa-${key.name}.json")

        return ResponseEntity(bytes, responseHeaders, HttpStatus.OK)
    }

    @DeleteMapping("/auth/v1/apikey/{id}")
    @ApiOperation("Delete Operation")
    fun delete(@PathVariable id: UUID) {
        apiKeyService.delete(apiKeyService.get(id))
    }

    @GetMapping("/auth/v1/apikey")
    @ApiOperation("Retrieve all Keys")
    fun findAll(): List<ApiKey> {
        return apiKeyService.findAll()
    }
}
