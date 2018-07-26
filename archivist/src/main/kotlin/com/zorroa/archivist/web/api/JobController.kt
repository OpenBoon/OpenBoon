package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.ExportFilter
import com.zorroa.archivist.service.ExportService
import com.zorroa.common.repository.KPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.IOException


@RestController
class JobController @Autowired constructor(
        val exportService: ExportService
) {

    @PostMapping(value = ["/api/v1/jobs/_search"])
    @Throws(IOException::class)
    fun getAll(@RequestBody(required = false) filter: ExportFilter,
               @RequestParam(value = "from", required = false) from: Int?,
               @RequestParam(value = "count", required = false) count: Int?): Any {
        return exportService.getAll(KPage(from, count), filter)
    }
}
