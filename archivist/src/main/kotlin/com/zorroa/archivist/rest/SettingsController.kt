package com.zorroa.archivist.rest

import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.domain.Setting
import com.zorroa.archivist.domain.SettingsFilter
import com.zorroa.archivist.service.SettingsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
class SettingsController @Autowired constructor(
        private val  settingsService: SettingsService
){

    @GetMapping(value = ["/api/v1/settings"])
    fun getAll(@RequestBody(required = false) filter: SettingsFilter?): List<Setting> {
        var filter = filter
        if (filter == null) {
            filter = SettingsFilter()
        }
        return settingsService.getAll(filter)
    }

    @GetMapping(value = ["/api/v1/settings/{name:.+}"])
    operator fun get(@PathVariable name: String): Setting {
        return settingsService.get(name)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).DEV) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/settings"])
    fun set(@RequestBody settings: Map<String, String>): Any {
        val count = settingsService.setAll(settings)
        return HttpUtils.status("settings", "update", count == settings.size)
    }
}
