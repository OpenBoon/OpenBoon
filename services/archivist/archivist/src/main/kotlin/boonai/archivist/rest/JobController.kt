package boonai.archivist.rest

import boonai.archivist.domain.CredentialsType
import boonai.archivist.domain.Job
import boonai.archivist.domain.JobFilter
import boonai.archivist.domain.JobSpec
import boonai.archivist.domain.JobUpdateSpec
import boonai.archivist.domain.TaskErrorFilter
import boonai.archivist.service.CredentialsService
import boonai.archivist.service.DependService
import boonai.archivist.service.JobService
import boonai.archivist.util.HttpUtils
import boonai.common.util.Json
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException
import java.util.UUID

@PreAuthorize("hasAuthority('DataQueueManage')")
@RestController
@Api(tags = ["Job"], description = "Operations for interacting with jobs.")
class JobController @Autowired constructor(
    val jobService: JobService,
    val credentialsService: CredentialsService,
    val dependService: DependService
) {

    @ApiOperation("Search for Jobs.")
    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun search(
        @ApiParam("Search filter.") @RequestBody(required = false) filter: JobFilter,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): Any {
        // Backwards compat
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return jobService.getAll(filter)
    }

    @ApiOperation(
        "Searches for a single Job.",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/jobs/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody filter: JobFilter): Job {
        return jobService.findOneJob(filter)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @ApiOperation("Create a Job.")
    @PostMapping(value = ["/api/v1/jobs"])
    @Throws(IOException::class)
    fun create(
        @ApiParam("Job to create.") @RequestBody spec: JobSpec
    ): Any {
        val job = jobService.create(spec)
        return jobService.get(job.id, forClient = true)
    }

    @ApiOperation("Update a Job.")
    @PutMapping(value = ["/api/v1/jobs/{id}"])
    @Throws(IOException::class)
    fun update(
        @ApiParam("UUID of the Job.") @PathVariable id: UUID,
        @ApiParam("Job updates.") @RequestBody spec: JobUpdateSpec
    ): Any {
        val job = jobService.get(id)
        jobService.updateJob(job, spec)
        return jobService.get(job.id, forClient = true)
    }

    @ApiOperation("Get a Job.")
    @GetMapping(value = ["/api/v1/jobs/{id}"])
    fun get(@ApiParam("UUID of the Job.") @PathVariable id: String): Any {
        return jobService.get(UUID.fromString(id), forClient = true)
    }

    @ApiOperation("Get a list of the Job's task errors.")
    @RequestMapping(value = ["/api/v1/jobs/{id}/taskerrors"], method = [RequestMethod.GET, RequestMethod.POST])
    fun getTaskErrors(
        @ApiParam("UUID of the Job.") @PathVariable id: UUID,
        @ApiParam("Search filter.") @RequestBody(required = false) filter: TaskErrorFilter?
    ): Any {
        val fixedFilter = if (filter == null) {
            TaskErrorFilter(jobIds = listOf(id))
        } else {
            filter.jobIds = listOf(id)
            filter
        }
        return jobService.getTaskErrors(fixedFilter)
    }

    @ApiOperation("Cancel a Job.")
    @PutMapping(value = ["/api/v1/jobs/{id}/_cancel"])
    @Throws(IOException::class)
    fun cancel(@ApiParam("UUID of the Job.") @PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "cancel", jobService.cancelJob(jobService.get(id)))
    }

    @ApiOperation("Resatart a Job.")
    @PutMapping(value = ["/api/v1/jobs/{id}/_restart"])
    @Throws(IOException::class)
    fun restart(@ApiParam("UUID of the Job.") @PathVariable id: UUID): Any {
        return HttpUtils.status("Job", id, "restart", jobService.restartJob(jobService.get(id)))
    }

    @ApiOperation("Retries all task in a Job that failed.")
    @PutMapping(value = ["/api/v1/jobs/{id}/_retryAllFailures"])
    @Throws(IOException::class)
    fun retryAllFailures(@ApiParam("UUID of the Job.") @PathVariable id: UUID): Any {
        val job = jobService.get(id)
        val result = jobService.retryAllTaskFailures(jobService.get(id))
        if (result > 0) {
            jobService.restartJob(job)
        }
        return HttpUtils.status("Job", id, "retryAllFailures", result > 0)
    }

    @PreAuthorize("hasAuthority('SystemProjectDecrypt')")
    @ApiOperation("Get credentials attached to Job", hidden = true)
    @GetMapping(value = ["/api/v1/jobs/{id}/_credentials/{type}"])
    fun getCredentials(
        @ApiParam("UUID of the Job") @PathVariable id: UUID,
        @ApiParam("Type of credentials") @PathVariable type: String
    ): Any {
        return Json.Mapper.readValue(
            credentialsService.getDecryptedBlobByJob(
                id,
                CredentialsType.valueOf(type.toUpperCase())
            ),
            Json.GENERIC_MAP
        )
    }

    @ApiOperation("Drop JobOnJob dependencies")
    @PostMapping(value = ["/api/v1/jobs/{id}/_drop_depends"])
    fun dropJobDependencies(
        @ApiParam("UUID of the Job") @PathVariable id: UUID
    ): Any {
        val job = jobService.get(id)
        return mapOf("dropped" to dependService.dropDepends(job))
    }
}
