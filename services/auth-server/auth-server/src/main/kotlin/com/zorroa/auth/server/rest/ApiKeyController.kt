package com.zorroa.auth.server.rest

import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.repository.ApiKeyCustomRepository
import com.zorroa.auth.server.repository.PagedList
import com.zorroa.auth.server.service.ApiKeyService
import com.zorroa.zmlp.util.Json
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import java.util.UUID
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@PreAuthorize("hasAuthority('ProjectManage')")
@Api(tags = ["API Key"], description = "Operations for managing API Keys.")
class ApiKeyController(
    val apiKeyService: ApiKeyService,
    val apikeyCustomRepository: ApiKeyCustomRepository
) {

    @PostMapping("/auth/v1/apikey")
    @ApiOperation("Create Api Key")
    fun create(@RequestBody spec: ApiKeySpec): ApiKey {
        return apiKeyService.create(spec)
    }

    @PutMapping("/auth/v1/apikey/{id}")
    @ApiOperation("Update Api Key")
    fun update(@PathVariable id: UUID, @RequestBody spec: ApiKeySpec): ApiKey {
        return apiKeyService.update(id, spec)
    }

    @GetMapping("/auth/v1/apikey/{id}")
    @ApiOperation("Create Api Key by Id")
    fun get(@PathVariable id: UUID): ApiKey {
        return apiKeyService.get(id)
    }

    @RequestMapping("/auth/v1/apikey/_findOne", method = [RequestMethod.GET, RequestMethod.POST])
    @ApiOperation("Get a single API Key")
    fun findOne(@RequestBody(required = false) filter: ApiKeyFilter?): ApiKey {
        return apiKeyService.findOne(filter ?: ApiKeyFilter())
    }

    @RequestMapping("/auth/v1/apikey/_search", method = [RequestMethod.GET, RequestMethod.POST])
    @ApiOperation("Search for API Keys")
    fun search(@RequestBody(required = false) filter: ApiKeyFilter?): PagedList<ApiKey> {
        return apiKeyService.search(filter ?: ApiKeyFilter())
    }

    @GetMapping("/auth/v1/apikey/{id}/_download")
    @ApiOperation("Download API Key")
    fun download(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val key = apikeyCustomRepository.getSigningKey(id)
        val bytes = Json.Mapper.writeValueAsBytes(key)

        val responseHeaders = HttpHeaders()
        responseHeaders.set("charset", "utf-8")
        responseHeaders.contentType = MediaType.valueOf("application/json")
        responseHeaders.contentLength = bytes.size.toLong()
        responseHeaders.set("Content-disposition", "attachment; filename=zorroa-${key.accessKey}.json")

        return ResponseEntity(bytes, responseHeaders, HttpStatus.OK)
    }

    @GetMapping("/auth/v1/apikey/{name}/_downloadByName")
    @ApiOperation("Download API Key")
    fun downloadNamed(@PathVariable name: String): ResponseEntity<ByteArray> {
        val key = apikeyCustomRepository.getSigningKey(name)
        val bytes = Json.Mapper.writeValueAsBytes(key)

        val responseHeaders = HttpHeaders()
        responseHeaders.set("charset", "utf-8")
        responseHeaders.contentType = MediaType.valueOf("application/json")
        responseHeaders.contentLength = bytes.size.toLong()
        responseHeaders.set("Content-disposition", "attachment; filename=zorroa-${key.accessKey}.json")

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

    @PostMapping("/auth/v1/apikey/_enable_project/{projectId}")
    fun enabledProject(@PathVariable projectId: UUID) {
        return apiKeyService.updateEnabledByProject(projectId, true)
    }

    @PostMapping("/auth/v1/apikey/_disable_project/{projectId}")
    fun disableProject(@PathVariable projectId: UUID) {
        return apiKeyService.updateEnabledByProject(projectId, false)
    }

    @DeleteMapping("/auth/v1/apikey/_delete_project/{projectId}")
    fun deleteByProject(@PathVariable projectId: UUID) {
        return apiKeyService.deleteByProject(projectId)
    }
}
