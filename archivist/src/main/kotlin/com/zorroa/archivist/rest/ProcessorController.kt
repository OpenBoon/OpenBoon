package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.archivist.service.ProcessorService
import com.zorroa.archivist.util.StaticUtils.UUID_REGEXP
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class ProcessorController @Autowired constructor(
        val processorService: ProcessorService) {


    @RequestMapping(value = ["/api/v1/processors/_search"], method = [RequestMethod.POST, RequestMethod.GET])
    fun search(@RequestBody(required = true) filter: ProcessorFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): KPagedList<Processor> {
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return processorService.getAll(filter)
    }

    @GetMapping(value = ["/api/v1/processors/{id:.+}"])
    fun get(@PathVariable(value="id") id: String): Processor {
        return if (UUID_REGEXP.matches(id)) {
            processorService.get(UUID.fromString(id))
        } else {
            processorService.get(id)
        }
    }
}