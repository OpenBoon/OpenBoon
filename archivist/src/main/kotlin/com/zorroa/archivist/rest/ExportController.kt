package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.ExportFileSpec
import com.zorroa.archivist.domain.ExportSpec
import com.zorroa.archivist.service.ExportService
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.util.copyInputToOuput
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(tags = ["Export"], description = "Operations for interacting with Exports.")
class ExportController @Autowired constructor(
    private val exportService: ExportService,
    private val jobService: JobService,
    private val fileServerProvider: FileServerProvider,
    private val fileStorageService: FileStorageService
) {

    @ApiOperation(value = "Create an Export")
    @PostMapping(value = ["/api/v1/exports"])
    fun create(@ApiParam(value = "Export to create.") @RequestBody spec: ExportSpec): Any {
        return exportService.create(spec)
    }

    @ApiOperation(value = "Get an Export.")
    @GetMapping(value = ["/api/v1/exports/{id}"])
    operator fun get(@ApiParam(value = "UUID of the Export.") @PathVariable id: UUID): Any {
        return jobService.get(id)
    }

    @ApiParam(value = "Create a File that belongs to an Export.")
    @PostMapping(value = ["/api/v1/exports/{id}/_files"])
    fun createExportFile(
        @ApiParam(value = "UUID of the Export.") @PathVariable id: UUID,
        @ApiParam(value = "File to create.") @RequestBody spec: ExportFileSpec
    ): Any {
        val job = jobService.get(id)
        return exportService.createExportFile(job, spec)
    }

    @ApiParam(value = "Get Files in an Export.")
    @GetMapping(value = ["/api/v1/exports/{id}/_files"])
    fun getExportFiles(@ApiParam(value = "UUID of the Export.") @PathVariable id: UUID): Any {
        val job = jobService.get(id)
        return exportService.getAllExportFiles(job)
    }

    @ApiParam(value = "Return an Export's File.")
    @GetMapping(value = ["/api/v1/exports/{id}/_files/{fileId}/_stream"])
    fun streamExportFile(
        req: HttpServletRequest,
        rsp: HttpServletResponse,
        @ApiParam(value = "UUID of the Export.") @PathVariable id: UUID,
        @ApiParam(value = "UUID of the File.") @PathVariable fileId: UUID
    ) {
        val file = exportService.getExportFile(fileId)
        val st = fileStorageService.get(file.path).getServableFile()
        val stat = st.getStat()

        rsp.contentType = stat.mediaType
        rsp.setContentLengthLong(stat.size)
        rsp.setHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        copyInputToOuput(st.getInputStream(), rsp.outputStream)
    }
}
