package com.zorroa.archivist.rest

import com.google.cloud.storage.HttpMethod
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.FileServerProvider
import com.zorroa.archivist.service.FileStat
import com.zorroa.archivist.service.FileStorageService
import com.zorroa.archivist.service.ImageService
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
@Timed
@Api(tags = ["File Storage"], description = "Operations for working with the file storage backend.")
class FileStorageController @Autowired constructor(
    private val fileStorageService: FileStorageService,
    private val fileServerProvider: FileServerProvider,
    private val imageService: ImageService
) {

    @ApiOperation("Create a File Storage object.")
    @RequestMapping("/api/v1/file-storage", method = [RequestMethod.POST, RequestMethod.GET])
    fun create(@ApiParam("Description of the file to be stored.") @RequestBody spec: FileStorageSpec): FileStorage {
        return fileStorageService.get(spec)
    }

    @ApiOperation("Get a File Storage object.")
    @GetMapping("/api/v1/file-storage/{id}")
    fun get(@ApiParam("UUID of the File Storage object.") @PathVariable id: String): FileStorage {
        return fileStorageService.get(id)
    }

    @ApiOperation("Returns statistics for the file stored at this File Storage location.")
    @GetMapping("/api/v1/file-storage/{id}/_stat")
    fun stat(@ApiParam("UUID of the File Storage object.") @PathVariable id: String): FileStat {
        val loc = fileStorageService.get(id)
        return fileServerProvider.getServableFile(loc.uri).getStat()
    }

    @ApiOperation("Returns a url where the file for this File Storage object can be downloaded.",
        notes = "If the storage location is a cloud provider this will be a signed url.")
    @GetMapping("/api/v1/file-storage/{id}/_download-uri")
    fun getDownloadURI(@ApiParam("UUID of the File Storage object.") @PathVariable id: String): Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.GET))
    }

    @ApiOperation("Returns a url where a file for this File Storage object can be uploaded.",
        notes = "If the storage location is a cloud provider this will be a signed url.")
    @GetMapping("/api/v1/file-storage/{id}/_upload-uri")
    fun getUploadURI(@ApiParam("UUID of the File Storage object.") @PathVariable id: String): Any {
        return mapOf("uri" to fileStorageService.getSignedUrl(id, HttpMethod.PUT))
    }

    @ApiOperation("Returns the file stored at this File Storage location.")
    @GetMapping("/api/v1/file-storage/{id}/_stream")
    fun stream(@ApiParam("UUID of the File Storage object.") @PathVariable id: String, rsp: HttpServletResponse) {
        val loc = fileStorageService.get(id)
        // At least try to handle the image watermarks for on-prem
        if (loc.mediaType.startsWith("image")) {
            imageService.serveImage(rsp, loc)
        } else {
            fileServerProvider.getServableFile(loc).copyTo(rsp)
        }
    }

    @Deprecated("See the /api/v1/file-storage/{id}/_stream endpoint", ReplaceWith("stream(id, rsp)"))
    @ApiOperation("DEPRECATED: Exists for v0.39 backwards compatibility only.")
    @RequestMapping(value = ["/api/v1/ofs/{id}"], method = [RequestMethod.GET])
    fun oldStream(@ApiParam("UUID of the OFS.") @PathVariable id: String, rsp: HttpServletResponse) {
        stream(id, rsp)
    }
}
