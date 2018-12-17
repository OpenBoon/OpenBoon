package com.zorroa.archivist.rest

import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.LockState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*


@PreAuthorize("hasAuthority(T( com.zorroa.security.Groups).SUPERADMIN)")
@RestController
class AnalystController @Autowired constructor(
        val analystService: AnalystService) {

    @PostMapping(value = ["/api/v1/analysts/_search"])
    fun search(@RequestBody filter: AnalystFilter) : Any {
        return analystService.getAll(filter)
    }

    @GetMapping(value = ["/api/v1/analysts/{id}"])
    fun get( @PathVariable id: UUID) : Analyst {
        return analystService.get(id)
    }

    @PutMapping(value = ["/api/v1/analysts/{id}/_lock"])
    fun setLockState(@PathVariable id: UUID, @RequestParam(value = "state", required = true) state: String) : Any {
        val newState = LockState.valueOf(state.toLowerCase().capitalize())
        val analyst = analystService.get(id)
        return HttpUtils.updated("analyst", analyst.id, analystService.setLockState(analyst, newState))
    }
}
