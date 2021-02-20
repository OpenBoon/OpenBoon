package boonai.archivist.rest

import boonai.archivist.config.ApplicationProperties
import boonai.archivist.domain.Analyst
import boonai.archivist.domain.AnalystFilter
import boonai.archivist.domain.LockState
import boonai.archivist.service.AnalystService
import boonai.archivist.util.HttpUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('SystemManage')")
@RestController
@Api(tags = ["Analyst"], description = "Operations for managing and interacting with the Analysts.")
class AnalystController @Autowired constructor(
    val analystService: AnalystService,
    val workQueue: AsyncListenableTaskExecutor,
    val properties: ApplicationProperties
) {

    @ApiOperation("Returns a list of Analysts matching the search filter.")
    @PostMapping(value = ["/api/v1/analysts/_search"])
    fun search(@ApiParam("Search filter.") @RequestBody filter: AnalystFilter): Any {
        return analystService.getAll(filter)
    }

    @ApiOperation(
        "Searches for a single Analyst",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/analysts/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody(required = false) filter: AnalystFilter): Analyst {
        return analystService.findOne(filter)
    }

    @ApiOperation("Returns info describing an Analyst.")
    @GetMapping(value = ["/api/v1/analysts/{id}"])
    fun get(@ApiParam("UUID of the Analyst.") @PathVariable id: UUID): Analyst {
        return analystService.get(id)
    }

    @ApiOperation(
        "Sets the lock state of an Analyst.",
        notes = "Locking an Analyst prevents it from picking up any new jobs."
    )
    @PutMapping(value = ["/api/v1/analysts/{id}/_lock"])
    fun setLockState(
        @ApiParam("UUID of the Analyst.") @PathVariable id: UUID,
        @ApiParam("State to set Analyst to.", allowableValues = "locked,unlocked")
        @RequestParam(value = "state", required = true) state: String
    ): Any {
        val newState = LockState.valueOf(state.toLowerCase().capitalize())
        val analyst = analystService.get(id)
        return HttpUtils.updated("analyst", analyst.id, analystService.setLockState(analyst, newState))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystController::class.java)
    }
}
