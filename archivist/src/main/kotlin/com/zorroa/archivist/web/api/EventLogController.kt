package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.EventLogSearch
import com.zorroa.archivist.service.EventLogService
import com.zorroa.sdk.domain.PagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.io.IOException

@PreAuthorize("hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).ADMIN)")
@RestController
class EventLogController @Autowired constructor(
        private val eventLogService: EventLogService
){

    @RequestMapping(value = ["/api/v1/eventlogs/{type}/_search"],
            method = [RequestMethod.GET, RequestMethod.POST])
    @Throws(IOException::class)
    fun search(
            @PathVariable type: String,
            @RequestBody search: EventLogSearch): PagedList<Map<String, Any>> {
        return eventLogService.getAll(type, search)
    }
}
