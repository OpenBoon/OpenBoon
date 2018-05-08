package com.zorroa.archivist.web.api

import com.zorroa.archivist.service.AnalystService
import com.zorroa.common.domain.Analyst
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@PreAuthorize("hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).DEV) || hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).ADMIN)")
@RestController
class AnalystController @Autowired constructor(
        private val analystService: AnalystService
) {
    @GetMapping(value = ["/api/v1/analysts"])
    fun getAll(
            @RequestParam(value = "page", required = false) page: Int?,
            @RequestParam(value = "count", required = false) count: Int?): PagedList<Analyst> {
        return analystService.getAll(Pager(page, count))
    }
}
