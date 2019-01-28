package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobUpdateSpec
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*


@RestController
@Timed
class JobController @Autowired constructor(
        val jobService: JobService,
        val dispatcherService: DispatcherService
) {

    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun search(@RequestBody(required = false) filter: JobFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): Any {
        // Backwards compat
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return jobService.getAll(filter)
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

    @RequestMapping(value = ["/api/v1/jobs/{id}/taskerrors"], method=[RequestMethod.GET, RequestMethod.POST])
    fun getTaskErrors(@PathVariable id: UUID, @RequestBody(required = false) filter: TaskErrorFilter?): Any {
        val fixedFilter = if (filter == null) {
            TaskErrorFilter(jobIds=listOf(id))
        }
        else {
            filter.jobIds = listOf(id)
            filter
        }
        return jobService.getTaskErrors(fixedFilter)
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_cancel"])
    @Throws(IOException::class)
    fun cancel(@PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "cancel", jobService.cancelJob(jobService.get(id)))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_restart"])
    @Throws(IOException::class)
    fun restart(@PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "restart", jobService.restartJob(jobService.get(id)))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_retryAllFailures"])
    @Throws(IOException::class)
    fun retryAllFailures(@PathVariable id: UUID): Any {
        val result = jobService.retryAllTaskFailures(jobService.get(id))
        return HttpUtils.status("Job", id, "retryAllFailures", result > 0)
    }
}
