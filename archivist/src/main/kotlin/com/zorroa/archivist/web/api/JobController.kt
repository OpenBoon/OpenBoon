package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobUpdateSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*


@RestController
class JobController @Autowired constructor(
        val jobService: JobService,
        val dispatcherService: DispatcherService
) {

    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun search(@RequestBody(required = false) filter: JobFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): Any {
        return jobService.getAll(Pager(from, count), filter)
    }

    @PostMapping(value = ["/api/v1/jobs"])
    @Throws(IOException::class)
    fun create(@RequestBody spec: JobSpec): Any {
        val job = jobService.create(spec)
        return jobService.get(job.id, forClient = true)
    }

    @PutMapping(value = ["/api/v1/jobs/{id}"])
    @Throws(IOException::class)
    fun update(@PathVariable id: UUID, @RequestBody spec: JobUpdateSpec): Any {
        val job = jobService.get(id)
        jobService.updateJob(job, spec)
        return jobService.get(job.id, forClient = true)
    }

    @GetMapping(value = ["/api/v1/jobs/{id}"])
    fun get(@PathVariable id: String): Any {
        return jobService.get(UUID.fromString(id), forClient = true)
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_cancel"])
    @Throws(IOException::class)
    fun cancel(@PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "cancel", jobService.cancelJob(jobService.get(id)))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_restart"])
    @Throws(IOException::class)
    fun restart(@PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "restart", jobService.restartCanceledJob(jobService.get(id)))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_retryAllFailures"])
    @Throws(IOException::class)
    fun retryAllFailures(@PathVariable id: UUID): Any {
        val result = jobService.retryAllTaskFailures(jobService.get(id))
        return HttpUtils.status("Job", id, "retryAllFailures", result > 0)
    }
}
