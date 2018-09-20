package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.service.StorageRouter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.*


@RestController
class ExportController @Autowired constructor(
        private val exportService: ExportService,
        private val jobService: JobService,
        private val storageRouter: StorageRouter
) {

    @PostMapping(value = ["/api/v1/exports"])
    fun create(@RequestBody spec: ExportSpec): Any {
        return exportService.create(spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}"])
    operator fun get(@PathVariable id: UUID): Any {
        return jobService.get(id)
    }

    @PostMapping(value = ["/api/v1/exports/{id}/_files"])
    fun createExportFile(@PathVariable id: UUID, @RequestBody spec: ExportFileSpec): Any {
        val job = jobService.get(id)
        return exportService.createExportFile(job, spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files"])
    fun getExportFiles(@PathVariable id: UUID): Any {
        val job = jobService.get(id)
        return exportService.getAllExportFiles(job)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files/{fileId}/_stream"])
    fun streamExportfile(@PathVariable id: UUID, @PathVariable fileId: UUID): ResponseEntity<InputStreamResource> {
        val file = exportService.getExportFile(fileId)
        return storageRouter.getObjectFile(URI.create(file.path)).getReponseEntity()
    }
}

