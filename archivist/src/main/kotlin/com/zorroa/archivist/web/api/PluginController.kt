package com.zorroa.archivist.web.api

import com.google.common.collect.ImmutableMap
import com.zorroa.archivist.domain.Processor
import com.zorroa.archivist.domain.ProcessorFilter
import com.zorroa.archivist.service.PluginService
import com.zorroa.sdk.client.exception.ArchivistWriteException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * Created by chambers on 6/29/16.
 */

@PreAuthorize("hasAuthority('group::developer') || hasAuthority('group::administrator')")
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

    @RequestMapping(value = ["/api/v1/processors"], method = [RequestMethod.GET])
    fun processors(@RequestBody(required = false) filter: ProcessorFilter?): List<Processor> {
        return if (filter == null) {
            pluginService.getAllProcessors()
        } else {
            pluginService.getAllProcessors(filter)
        }
    }
}
