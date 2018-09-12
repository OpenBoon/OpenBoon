package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.JobFilter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


@RestController
class JobController @Autowired constructor(
        val exportService: ExportService,
        val jobService: JobService,
        val dispatcherService: DispatcherService
) {

    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun getAll(@RequestBody(required = false) filter: JobFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): Any {
        return jobService.getAll(Pager(from, count), filter)
    }
}
