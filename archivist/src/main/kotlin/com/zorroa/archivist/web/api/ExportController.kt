package com.zorroa.archivist.web.api

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.archivist.service.EventLogService
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.service.SearchService
import com.zorroa.sdk.client.exception.ArchivistReadException
import com.zorroa.sdk.search.AssetFilter
import com.zorroa.sdk.search.AssetSearch
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Paths

@RestController
class ExportController @Autowired constructor(
        private val exportService: ExportService,
        private val jobService: JobService,
        private val searchService: SearchService,
        private val logService: EventLogService
) {

    @PostMapping(value = ["/api/v1/exports"])
    fun create(@RequestBody spec: ExportSpec): Any {
        return exportService.create(spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}"])
    operator fun get(@PathVariable id: Int): Any {
        return jobService[id]
    }

    @PostMapping(value = ["/api/v1/exports/{id}/_files"])
    fun createExportFile(@PathVariable id: Int, @RequestBody spec: ExportFileSpec): Any {
        val job = jobService[id]
        return exportService.createExportFile(job, spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files"])
    fun getExportFiles(@PathVariable id: Int): Any {
        val job = jobService[id]
        return exportService.getAllExportFiles(job)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files/{fileId}/_stream"])
    fun streamExportfile(@PathVariable id: Int, @PathVariable fileId: Long): ResponseEntity<FileSystemResource> {
        val file = exportService.getExportFile(fileId)
        val job = jobService[id]

        /**
         * Don't let people download other people's exports, as its not possible
         * to know if they have access to each individual file.
         */
        if (job.jobId as Long != file.jobId) {
            throw ArchivistReadException("Invalid export file")
        }
        if (job.user.id != SecurityUtils.getUser().id) {
            throw ArchivistReadException("Invalid export for " + SecurityUtils.getUsername())
        }
        if (job.state != JobState.Finished) {
            throw ArchivistReadException("Export is not complete.")
        }

        logExportDownload(id)

        val path = Paths.get(job.rootPath).resolve("exported").resolve(file.name)

        val headers = HttpHeaders()
        headers.add("content-disposition", "attachment; filename=" + file.name)
        headers.contentType = MediaType.valueOf(file.mimeType)
        headers.contentLength = file.size
        return ResponseEntity(FileSystemResource(path.toFile()), headers, HttpStatus.OK)
    }

    private fun logExportDownload(id: Int) {
        val ids = Lists.newArrayList<String>()
        val search = AssetSearch()
                .setFields(arrayOf())
                .setFilter(AssetFilter()
                        .addToTerms("link.export.id", id.toString()))

        searchService.scanAndScroll(search, 10000).mapTo(ids) { it.id }
        logService.logAsync(UserLogSpec.build(LogAction.Export, "asset", *ids.toTypedArray()))
    }
}
