package com.zorroa.analyst.controller

import com.zorroa.analyst.service.JobService
import com.zorroa.common.domain.Job
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobState
import com.zorroa.common.repository.KPagedList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class JobController @Autowired constructor(
        val jobService: JobService
){

    /**
     * Called when a job is finished.
     */
    @PutMapping("/api/v1/jobs/{id}/_finish")
    fun finish(@PathVariable id: UUID) {
        val job = jobService.get(id)
        jobService.stop(job, JobState.SUCCESS)
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
