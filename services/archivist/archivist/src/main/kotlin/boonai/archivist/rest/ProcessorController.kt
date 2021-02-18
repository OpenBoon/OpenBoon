package boonai.archivist.rest

import boonai.archivist.domain.Processor
import boonai.archivist.domain.ProcessorFilter
import boonai.archivist.repository.KPagedList
import boonai.archivist.service.ProcessorService
import boonai.archivist.util.StaticUtils.UUID_REGEXP
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('SystemManage')")
@RestController
@Api(tags = ["Processor"], description = "Operations for interacting with Processors.")
class ProcessorController @Autowired constructor(
    val processorService: ProcessorService
) {

    @ApiOperation("Search for Pipelines.")
    @RequestMapping(value = ["/api/v1/processors/_search"], method = [RequestMethod.POST, RequestMethod.GET])
    fun search(
        @ApiParam("Search filter.") @RequestBody(required = true) filter: ProcessorFilter,
        @ApiParam("Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam("Number of results per page.") @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<Processor> {
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return processorService.getAll(filter)
    }

    @ApiOperation(
        "Search for a single Processor.",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @PostMapping(value = ["/api/v1/processors/_findOne"])
    fun findOne(@ApiParam("Search filter.") @RequestBody filter: ProcessorFilter): Processor {
        return processorService.findOne(filter)
    }

    @ApiOperation("Get a Processor.")
    @GetMapping(value = ["/api/v1/processors/{id:.+}"])
    fun get(@ApiParam("UUID of the Processor.") @PathVariable(value = "id") id: String): Processor {
        return if (UUID_REGEXP.matches(id)) {
            processorService.get(UUID.fromString(id))
        } else {
            processorService.get(id)
        }
    }
}
