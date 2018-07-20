package com.zorroa.analyst.controller

import com.zorroa.analyst.service.AssetService
import com.zorroa.analyst.service.JobService
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.domain.JobState
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class AssetController @Autowired constructor(
        val jobService: JobService,
        val assetService: AssetService
){

    @PostMapping("/api/v1/assets/{id}")
    fun update(@PathVariable id: UUID, @RequestBody doc: Document,
               @RequestParam("job", required = false) jobId: String?) : Any {

        if (jobId != null) {
            try {
                logger.info("Stopping job {}", jobId)
                val job = jobService.get(UUID.fromString(jobId))
                jobService.stop(job, JobState.SUCCESS)
            } catch (e: Exception) {
                logger.warn("Unable to stop job, {}", jobId, e)
            }
        }

        if (!doc.attrExists("zorroa.organizationId")) {
            throw PreconditionFailedException("Asset $id has no organization Id, cannot index")
        }
        else {
            val asset = Asset(id, UUID.fromString(doc.getAttr("zorroa.organizationId")))
            assetService.storeAndReindex(asset, doc)
        }

        return RestResponse(HttpMethod.POST,  "/api/v1/assets/$id", true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobController::class.java)
    }
}
