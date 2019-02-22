package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.copyInputToOuput
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@Timed
class ExportController @Autowired constructor(
        private val exportService: ExportService,
        private val jobService: JobService,
        private val fileServerProvider: FileServerProvider,
        private val fileStorageService: FileStorageService
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
    fun streamExportFile(req: HttpServletRequest, rsp: HttpServletResponse, @PathVariable id: UUID, @PathVariable fileId: UUID) {
        val file = exportService.getExportFile(fileId)
        val st = fileStorageService.get(file.path).getServableFile()
        val stat = st.getStat()

        rsp.contentType = stat.mediaType
        rsp.setContentLengthLong(stat.size)
        rsp.setHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        copyInputToOuput(st.getInputStream(), rsp.outputStream)
    }
}

