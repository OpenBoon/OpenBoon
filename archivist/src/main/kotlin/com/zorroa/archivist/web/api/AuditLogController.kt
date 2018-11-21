package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuditLogController @Autowired constructor(val auditLogDao : AuditLogDao) {

    @PostMapping(value="/api/v1/auditlog/_search")
    fun getAll(@RequestBody filter: AuditLogFilter) : KPagedList<AuditLogEntry> {
        return auditLogDao.getAll(filter)
    }
}