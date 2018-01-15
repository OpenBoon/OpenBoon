package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.EventLogSearch
import com.zorroa.archivist.service.EventLogService
import com.zorroa.sdk.domain.PagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.io.IOException

@PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
@RestController
class EventLogController @Autowired constructor(
        private val eventLogService: EventLogService
){

    @PostMapping(value = ["/api/v1/eventlogs/{type}/_search"])
    @Throws(IOException::class)
    fun search(
            @PathVariable type: String,
            @RequestBody search: EventLogSearch): PagedList<Map<String, Any>> {
        return eventLogService!!.getAll(type, search)
    }
}
