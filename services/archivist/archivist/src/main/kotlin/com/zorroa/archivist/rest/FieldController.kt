package com.zorroa.archivist.rest

import com.zorroa.archivist.service.IndexRoutingService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FieldController(
        val indexRoutingService: IndexRoutingService
) {

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/fields/_mapping"])
    fun mapping(): Map<String, Any> {
        return indexRoutingService.getProjectRestClient().getMapping()
    }
}