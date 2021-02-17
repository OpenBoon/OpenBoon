package boonai.archivist.rest

import boonai.archivist.domain.Credentials
import boonai.archivist.domain.CredentialsSpec
import boonai.archivist.domain.CredentialsUpdate
import boonai.archivist.service.CredentialsService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('AssetsImport')")
@RestController
@Api(tags = ["Credentials"], description = "Operations for managing Credentials.")
class CredentialsController constructor(
    val credentialsService: CredentialsService
) {

    @PreAuthorize("hasAuthority('ProjectManage')")
    @ApiOperation("Creat new  Credentials")
    @PostMapping(value = ["/api/v1/credentials"])
    fun create(@RequestBody spec: CredentialsSpec): Credentials {
        return credentialsService.create(spec)
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @ApiOperation("Get Credentials by ID.")
    @GetMapping(value = ["/api/v1/credentials/{id}"])
    fun get(@PathVariable id: UUID): Credentials {
        return credentialsService.get(id)
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @DeleteMapping(value = ["/api/v1/credentials/{id}"])
    @ApiOperation("Delete Credentials by ID.")
    fun delete(@PathVariable id: UUID): Any {
        credentialsService.delete(id)
        return HttpUtils.deleted("credentials", id, true)
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @ApiOperation("Update Credentials")
    @PutMapping(value = ["/api/v1/credentials/{id}"])
    fun update(@PathVariable id: UUID, @RequestBody update: CredentialsUpdate): Credentials {
        return credentialsService.update(id, update)
    }

    @PreAuthorize("hasAuthority('SystemProjectDecrypt')")
    @ApiOperation("Get decrypted credentials blob", hidden = true)
    @GetMapping(value = ["/api/v1/credentials/{id}/_download"])
    fun download(@PathVariable id: UUID): String {
        return credentialsService.getDecryptedBlob(id)
    }
}
