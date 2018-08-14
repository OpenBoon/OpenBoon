package com.zorroa.analyst.controller

import com.zorroa.analyst.service.JobRegistryService
import com.zorroa.analyst.service.JobService
import com.zorroa.analyst.service.SchedulerService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import com.zorroa.common.domain.JobState
import com.zorroa.common.repository.KPagedList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class JobController @Autowired constructor(
        val jobRegistryService: JobRegistryService,
        val jobService: JobService,
        val schedulerService: SchedulerService
){

    /**
     * Create a new job.
     */
    @PostMapping("/api/v1/jobs")
    fun create(@RequestBody spec: JobSpec) : Job {
        return jobRegistryService.launchJob(spec)
    }

    /**
     * Called by a job when the job is finished.  If the job crashes
     * then the job will be in an orphan state.
     */
    @PutMapping("/api/v1/jobs/{id}/_finish")
    fun finish(@PathVariable id: UUID) {
        val job = jobService.get(id)
        jobService.stop(job, JobState.Success)
    }

    /**
     * Retry the given job.
     */
    @PutMapping("/api/v1/jobs/{id}/_retry")
    fun retry(@PathVariable id: UUID) {
        val job = jobService.get(id)
        schedulerService.retry(job)
    }

    /**
     * Kill the given job and set to failed state.
     */
    @PutMapping("/api/v1/jobs/{id}/_kill")
    fun kill(@PathVariable id: UUID) {
        val job = jobService.get(id)
        schedulerService.retry(job)
    }

    @GetMapping("/api/v1/jobs")
    fun getAll(@RequestBody filter: JobFilter) : KPagedList<Job> {
        return jobService.getAll(filter)
    }

    @GetMapping("/api/v1/jobs/{id}")
    fun get(@PathVariable id: UUID) : Job {
        return jobService.get(id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobController::class.java)
    }
}
