package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FileTypes
import com.zorroa.archivist.util.FileUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FileTypesController {

    @GetMapping("/api/v1/file-types/images")
    fun getImageTypes(): Collection<String> {
        return FileTypes.image.toList().sorted()
    }

    @GetMapping("/api/v1/file-types/videos")
    fun getVideoTypes(): Collection<String> {
        return FileTypes.video.toList().sorted()
    }

    @GetMapping("/api/v1/file-types/documents")
    fun getDocumentTypes(): Collection<String> {
        return FileTypes.doc.toList().sorted()
    }

    @GetMapping("/api/v1/file-types/all")
    fun getAllTypes(): Collection<String> {
        return FileTypes.all.toList().sorted()
    }

    @GetMapping("/api/v1/media-type/{ext}")
    fun getMediaType(@PathVariable("ext") ext: String): Map<String, String> {
        return mapOf("media-type" to FileUtils.getMediaType("foo.$ext"))
    }
}
