package com.zorroa.archivist.web.api

import com.zorroa.archivist.service.AnalystService
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.AnalystSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@PreAuthorize("hasAuthority(T( com.zorroa.security.Groups).SUPERADMIN)")
@RestController
class AnalystController @Autowired constructor(
        val analystService: AnalystService) {

    @PostMapping(value = ["/api/v1/analysts/_search"])
    fun search(@RequestBody filter: AnalystFilter) : Any {
        return analystService.getAll(filter)
    }
}
