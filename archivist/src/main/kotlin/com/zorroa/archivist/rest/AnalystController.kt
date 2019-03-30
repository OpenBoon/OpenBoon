package com.zorroa.archivist.rest

import com.zorroa.archivist.service.AnalystService
import com.zorroa.archivist.service.ClusterLockService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.Analyst
import com.zorroa.common.domain.AnalystFilter
import com.zorroa.common.domain.LockState
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*


@PreAuthorize("hasAuthority(T( com.zorroa.security.Groups).SUPERADMIN)")
@RestController
@Timed
class AnalystController @Autowired constructor(
        val analystService: AnalystService,
        val workQueue: AsyncListenableTaskExecutor,
        val clusterLockService: ClusterLockService) {

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

    /**
     * Initiate a processor scan.  If the processor-scan key is locked, then the "success"
     * property on the response body is set to False.  This means there is an active
     * scan already running and the request was ignored.
     */
    @PostMapping(value = ["/api/v1/analysts/_processor_scan"])
    fun processorScan(): Any {
        val locked = clusterLockService.isLocked("processor-scan")
        if (!locked) {
            workQueue.execute {
                analystService.doProcessorScan()
            }
        }
        return HttpUtils.status("processor", "scan", !locked)
    }
}
