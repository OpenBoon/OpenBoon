package com.zorroa.archivist.rest

import com.zorroa.archivist.repository.FileQueueDao
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Api(tags = ["File Queue"], description = "Inspect the File Queue.")
class FileQueueController @Autowired constructor(
    private val fileQueueDao: FileQueueDao
) {

    @ApiOperation("Gets the meters that drive load balancing jobs between organizations.",
        notes = "This endpoint will expose organization names. This endpoint is intended for internal use.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
    @GetMapping(value = ["/api/v1/file-queue/_meters"])
    fun getOrganizationMeters(): Any {
        return fileQueueDao.getOrganizationMeters()
    }
}
