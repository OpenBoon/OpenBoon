package com.zorroa.archivist.web.api

import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getUserId
import com.zorroa.archivist.service.JobExecutorService
import com.zorroa.archivist.service.JobService
import com.zorroa.sdk.client.exception.ArchivistWriteException
import com.zorroa.sdk.domain.PagedList
import com.zorroa.sdk.domain.Pager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.io.IOException
import javax.validation.Valid

/**
 * Unified controller for manipulating jobs of any type (import, export, etc)
 */
@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
@RestController
class JobController @Autowired constructor(
        private val jobService: JobService,
        private val jobExecutorService: JobExecutorService
) {

    @PutMapping(value = ["/api/v1/jobs/{id}/_cancel"])
    @Throws(IOException::class)
    fun cancel(@PathVariable id: Int): Any {
        return HttpUtils.status("job", id, "cancel",
                jobExecutorService.cancelJob(jobService[id]))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_restart"])
    @Throws(IOException::class)
    fun restart(@PathVariable id: Int): Any {
        return HttpUtils.status("job", id, "restart",
                jobExecutorService.restartJob(jobService[id]))
    }

    @PutMapping(value = ["/api/v1/jobs/{id}/_retryAllFailures"])
    @Throws(IOException::class)
    fun retryAllFailures(@PathVariable id: Int): Any {
        jobExecutorService.retryAllFailures(jobService[id])
        return HttpUtils.status("job", id, "retry-all-failures", true)
    }

    /**
     * Currently only called by cloud proxy to continue processing of an asset.
     *
     * @param spec
     * @return
     * @throws IOException
     */
    @PostMapping(value = ["/api/v1/jobs/{id}/_append"])
    @Throws(IOException::class)
    fun continueImport(@RequestBody spec: TaskSpecV): Any {
        val job = jobService[spec.jobId]
        if (job.user.id != getUserId()) {
            throw IllegalArgumentException("Invalid user for appending to job")
        }
        return jobService.continueImportTask(spec)
    }

    @GetMapping(value = ["/api/v1/jobs/{id}"])
    @Throws(IOException::class)
    operator fun get(@PathVariable id: Int): Any {
        return jobService[id]
    }

    @GetMapping(value = ["/api/v1/jobs/{id}/tasks"])
    @Throws(IOException::class)
    fun getTasks(@PathVariable id: Int,
                 @RequestParam(value = "from", required = false) from: Int?,
                 @RequestParam(value = "count", required = false) count: Int?): Any {
        return jobService.getAllTasks(id, Pager(from, count))
    }


    @PostMapping(value = ["/api/v2/jobs/{id}/tasks"])
    @Throws(IOException::class)
    fun getTasksV2(@PathVariable id: Int,
                   @RequestBody filter: TaskFilter,
                   @RequestParam(value = "from", required = false) from: Int?,
                   @RequestParam(value = "count", required = false) count: Int?): Any {
        return jobService.getAllTasks(id, Pager(from, count), filter)
    }

    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun getAll(@RequestBody(required = false) filter: JobFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): PagedList<Job> {
        return jobService.getAll(Pager(from, count), filter)
    }

    @PostMapping(value = ["/api/v1/jobs"])
    @Throws(IOException::class)
    fun launch(@Valid @RequestBody spec: JobSpecV, valid: BindingResult): Any {
        if (valid.hasErrors()) {
            throw ArchivistWriteException(HttpUtils.getBindingErrorString(valid))
        }
        return jobService.launch(spec)
    }
}
