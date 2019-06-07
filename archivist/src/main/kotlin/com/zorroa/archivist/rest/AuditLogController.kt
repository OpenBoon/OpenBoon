package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
@Api(tags = ["Audit Log"], description = "Access to Audit Logs.")
class AuditLogController @Autowired constructor(val auditLogDao: AuditLogDao) {

    @ApiOperation(value = "Search for Audit Logs.")
    @PostMapping(value = ["/api/v1/auditlog/_search"])
    fun getAll(
        @ApiParam(value = "Search filter used to get Audit Logs.") @RequestBody filter: AuditLogFilter
    ): KPagedList<AuditLogEntry> {
        return auditLogDao.getAll(filter)
    }

    @ApiOperation(value = "Searches for a single Audit Log.",
        notes = "Throws an error if more than 1 result is returned based on the given filter.")
    @PostMapping(value = ["/api/v1/auditlog/_findOne"])
    fun findOne(
        @ApiParam(value = "Search filter used to get Audit Log.") @RequestBody filter: AuditLogFilter
    ): AuditLogEntry {
        return auditLogDao.findOne(filter)
    }
}
