package com.zorroa.archivist.web.api

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.LfsRequest
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.service.LocalFileSystem
import com.zorroa.sdk.filesystem.ObjectFileSystem
import com.zorroa.sdk.util.FileUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.HandlerMapping
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@RestController
class FileSystemController @Autowired constructor(
        private val objectFileSystem: ObjectFileSystem,
        private val localFileSystem: LocalFileSystem,
        private val jobService: JobService
) {

    @RequestMapping(value = ["/api/v1/ofs/_exists"], method = [RequestMethod.POST])
    @Throws(IOException::class)
    fun fileExists(@RequestBody path: Map<String, String>): Any {
        val file = path["path"]
        if (file == null) {
            return ImmutableMap.of("result", false)
        } else {
            val f = File(FileUtils.normalize(file))
            return ImmutableMap.of("result", f.exists())
        }
    }

    @RequestMapping(value = ["/api/v1/ofs/{type}/**"], method =  [RequestMethod.GET])
    @ResponseBody
    @Throws(IOException::class)
    fun getFile(@PathVariable type: String, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<InputStreamResource> {
        response.setHeader("Cache-Control", "public")

        val path = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
        val bestMatchPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String

        val apm = AntPathMatcher()
        val id = type + "/" + FileUtils.filename(apm.extractPathWithinPattern(bestMatchPattern, path))
        val file = objectFileSystem.get(id)

        return if (!file.exists()) {
            ResponseEntity.notFound().build()
        } else ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(Files.size(file.file.toPath()))
                .body(InputStreamResource(FileInputStream(file.file)))
    }

    class ProxyUpload {
        var files: List<MultipartFile>? = null
    }

    /**
     * This method is used by cloud proxy
     *
     * @param upload
     * @return
     * @throws IOException
     */
    @RequestMapping(value = ["/api/v1/ofs/{type}"], method = [RequestMethod.POST])
    @Throws(IOException::class)
    fun proxyUpload(@PathVariable type: String, upload: ProxyUpload): Any {

        if (upload.files == null || upload.files!!.isEmpty()) {
            return HttpUtils.status("ofs", "upload", false)
        }

        if (!StringUtils.isAlphanumeric(type)) {
            return HttpUtils.status("ofs", "upload", false)
        }

        try {
            for (file in upload.files!!) {
                val id = type + "/" + file.originalFilename
                val of = objectFileSystem!!.get(id)
                of.mkdirs()

                val dstFile = of.file
                if (!dstFile.exists()) {
                    Files.copy(file.inputStream, dstFile.toPath())
                }
            }
            return HttpUtils.status("proxy", "upload", true)
        } catch (e: Exception) {
            logger.warn("Failed to upload proxies", e)
        }

        return HttpUtils.status("proxy", "upload", false)
    }

    @RequestMapping(value = ["/api/v1/lfs"], method = [RequestMethod.POST])
    @Throws(IOException::class)
    fun localFiles(@RequestBody req: LfsRequest): Any {
        return localFileSystem!!.listFiles(req)
    }

    @RequestMapping(value = ["/api/v1/lfs/_suggest"], method = [RequestMethod.POST])
    @Throws(IOException::class)
    fun localFilesSuggest(@RequestBody req: LfsRequest): List<String> {
        return localFileSystem!!.suggest(req)
    }

    @RequestMapping(value = ["/api/v1/lfs/_exist"], method = [RequestMethod.POST])
    @Throws(IOException::class)
    fun localFileExists(@RequestBody req: LfsRequest): Any {
        return HttpUtils.exists(req.path, localFileSystem!!.exists(req)!!)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(FileSystemController::class.java)
    }
}
