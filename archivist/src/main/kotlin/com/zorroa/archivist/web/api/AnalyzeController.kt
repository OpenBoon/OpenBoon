package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.AnalyzeSpec
import com.zorroa.archivist.service.AnalyzeService
import com.zorroa.sdk.util.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

/**
 * Created by chambers on 5/15/17.
 */
@RestController
class AnalyzeController @Autowired constructor(
        private val analyzeService: AnalyzeService
) {

    @PostMapping(value = ["/api/v1/analyze/_files"], consumes = ["multipart/form-data"])
    @ResponseBody
    @Throws(IOException::class)
    fun analyzeUpload(@RequestParam("files") files: Array<MultipartFile>,
                      @RequestParam("body") body: String): Any {
        val spec = Json.deserialize(body, AnalyzeSpec::class.java)
        return analyzeService.analyze(spec, files)
    }

    @PostMapping(value = ["/api/v1/analyze/_assets"])
    @ResponseBody
    @Throws(IOException::class)
    fun analyzeAssets(@RequestBody spec: AnalyzeSpec): Any {
        return analyzeService.analyze(spec, null)
    }
}
