package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.JobDao
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.JobService
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.domain.JobSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.io.IOException
import java.util.*
import javax.validation.Valid


@RestController
class JobController @Autowired constructor(
        val jobDao: JobDao,
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

    @PostMapping(value = ["/api/v1/jobs"])
    @Throws(IOException::class)
    fun create(@RequestBody spec: JobSpec): Any {
        val job = jobService.create(spec)
        return jobDao.getForClient(job.id)
    }

    @GetMapping(value = ["/api/v1/jobs/{id}"])
    fun get(@PathVariable id: String): Any {
        return jobService.get(UUID.fromString(id))
    }
}
