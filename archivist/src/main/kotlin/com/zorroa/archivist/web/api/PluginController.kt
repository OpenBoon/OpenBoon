package com.zorroa.archivist.web.api

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.archivist.service.PluginService
import com.zorroa.archivist.util.KJson.UUID_REGEXP
import com.zorroa.sdk.client.exception.ArchivistWriteException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

/**
 * Created by chambers on 6/29/16.
 */

@PreAuthorize("hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).DEV) || hasAuthority(T(com.zorroa.archivist.sdk.security.Groups).ADMIN)")
@RestController
class PluginController @Autowired constructor(
        private val pluginService: PluginService
){

    @ResponseBody
    @RequestMapping(value = ["/api/v1/plugins"], method = [RequestMethod.POST])
    fun handlePluginUpload(@RequestParam("file") file: MultipartFile): Any {
        if (!file.isEmpty) {
            if (!file.originalFilename.endsWith("-plugin.zip")) {
                throw RuntimeException("The plugin package name must end with -plugin.zip")
            }
            val p = pluginService.installPlugin(file)
            return ImmutableMap.of(
                    "name", p.name,
                    "description", p.description,
                    "version", p.version,
                    "publisher", p.publisher,
                    "language", p.language)
        } else {
            throw ArchivistWriteException("Failed to handle plugin zip file")
        }
    }

    @RequestMapping(value = ["/api/v1/processors"], method = [RequestMethod.GET, RequestMethod.POST])
    fun getProcessors(@RequestBody(required = false) filter: ProcessorFilter?): List<Processor> {
        return if (filter == null) {
            pluginService.getAllProcessors()
        } else {
            pluginService.getAllProcessors(filter)
        }
    }

    @GetMapping(value = "/api/v1/processors/{id:.+}")
    fun getProcessor(@PathVariable id: String): Processor {
        return if (UUID_REGEXP.matches(id)) {
            pluginService.getProcessor(UUID.fromString(id))
        }
        else {
            pluginService.getProcessor(id)
        }
    }
}
