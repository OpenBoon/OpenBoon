package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.service.ExportService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.*

@RestController
class ExportController @Autowired constructor(
        private val exportService: ExportService
) {

    @PostMapping(value = ["/api/v1/exports"])
    fun create(@RequestBody spec: ExportSpec): Any {
        return exportService.create(spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}"])
    fun get(@PathVariable id: UUID): Any {
        return exportService.get(id)
    }

    @PostMapping(value = ["/api/v1/exports/{id}/_files"])
    fun createExportFile(@PathVariable id: UUID, @RequestBody spec: ExportFileSpec): Any {
        val export = exportService.get(id)
        return exportService.createExportFile(export, spec)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files"])
    fun getExportFiles(@PathVariable id: UUID): Any {
        val export = exportService.get(id)
        return exportService.getAllExportFiles(export)
    }

    @GetMapping(value = ["/api/v1/exports/{id}/_files/{fileId}/_stream"])
    fun streamExportfile(@PathVariable id: UUID, @PathVariable fileId: UUID): ResponseEntity<InputStreamResource> {
        val file = exportService.getExportFile(fileId)

        val headers = HttpHeaders()
        headers.add("content-disposition", "attachment; filename=" + file.name)
        headers.contentType = MediaType.valueOf(file.mimeType)
        headers.contentLength = file.size

        val isr = InputStreamResource(BufferedInputStream(FileInputStream(file.path)))
        return ResponseEntity(isr, headers, HttpStatus.OK)
    }
}
