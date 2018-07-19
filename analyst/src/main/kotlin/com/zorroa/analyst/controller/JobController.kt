package com.zorroa.analyst.controller

import com.zorroa.analyst.service.AssetService
import com.zorroa.analyst.service.JobService
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class JobController @Autowired constructor(
        val jobService: JobService,
        val assetService: AssetService
){

    @PostMapping("/api/v1/jobs/{id}/result")
    fun finish(@PathVariable id: UUID, @RequestBody doc: Document) {
        val job = jobService.get(id)
        val asset = Asset(job.assetId, job.organizationId, job.attrs)

        if(jobService.finish(job)) {
            assetService.storeAndReindex(asset, doc)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobController::class.java)
    }
}
