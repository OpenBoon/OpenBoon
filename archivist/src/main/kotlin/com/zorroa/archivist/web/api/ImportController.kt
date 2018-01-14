package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.ImportSpec
import com.zorroa.archivist.domain.UploadImportSpec
import com.zorroa.archivist.service.ImportService
import com.zorroa.archivist.service.JobService
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
class ImportController @Autowired constructor(
        private val importService: ImportService,
        private val jobService: JobService
){

    @PostMapping(value = "/api/v1/imports/_upload", consumes = ["multipart/form-data"])
    @ResponseBody
    fun upload(@RequestParam("files") files: Array<MultipartFile>,
               @RequestParam("body") body: String): Any {
        val spec = Json.deserialize(body, UploadImportSpec::class.java)
        return importService.create(spec, files)
    }

    @PostMapping(value = "/api/v1/imports")
    @Throws(IOException::class)
    fun create(@RequestBody spec: ImportSpec): Any {
        return importService.create(spec)
    }

    @GetMapping(value = "/api/v1/imports/{id}")
    @Throws(IOException::class)
    operator fun get(@PathVariable id: Int?): Any {
        return jobService[id!!]
    }
}
