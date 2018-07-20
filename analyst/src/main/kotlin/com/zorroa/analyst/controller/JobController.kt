package com.zorroa.analyst.controller

import com.zorroa.analyst.service.JobService
import com.zorroa.common.domain.JobState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class JobController @Autowired constructor(
        val jobService: JobService
){

    @PutMapping("/api/v1/jobs/{id}/_stop")
    fun stop(@PathVariable id: UUID) {
        val job = jobService.get(id)
        jobService.stop(job, JobState.SUCCESS)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobController::class.java)
    }
}
