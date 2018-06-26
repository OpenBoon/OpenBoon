package com.zorroa.irm.studio.controller

import com.zorroa.common.domain.Document
import com.zorroa.irm.studio.service.AssetService
import com.zorroa.irm.studio.service.JobService
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

        if(jobService.finish(job, doc)) {
            assetService.storeAndReindex(job.organizationId, doc)
        }
    }
}
