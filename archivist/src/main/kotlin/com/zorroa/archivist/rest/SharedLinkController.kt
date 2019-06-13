package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.service.SharedLinkService
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
import javax.validation.Valid

@RestController
@Timed
@Api(tags = ["Shared Link"], description = "Operations for interacting with Shared Links.")
class SharedLinkController @Autowired constructor(
    private val sharedLinkService: SharedLinkService
) {

    @ApiOperation("Create a Shared Link.")
    @PostMapping(value = ["/api/v1/shared_link"])
    fun create(@ApiParam("Shared Link to create.") @Valid @RequestBody spec: SharedLinkSpec): SharedLink {
        return sharedLinkService.create(spec)
    }

    @ApiOperation("Get a Shared Link.")
    @GetMapping(value = ["/api/v1/shared_link/{id}"])
    fun get(@ApiParam("UUID of the Shared Link.") @PathVariable id: UUID): SharedLink {
        return sharedLinkService.get(id)
    }
}
