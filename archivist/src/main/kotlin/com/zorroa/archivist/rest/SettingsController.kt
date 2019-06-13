package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Setting
import com.zorroa.archivist.domain.SettingsFilter
import com.zorroa.archivist.service.SettingsService
import com.zorroa.archivist.util.HttpUtils
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
@Api(tags = ["Settings"], description = "Operations for interacting with Settings.")
class SettingsController @Autowired constructor(
    private val settingsService: SettingsService
) {

    @ApiOperation("Gets all Settings.")
    @GetMapping(value = ["/api/v1/settings"])
    fun getAll(@ApiParam("Search filter.") @RequestBody(required = false) filter: SettingsFilter?): List<Setting> {
        var filter = filter
        if (filter == null) {
            filter = SettingsFilter()
        }
        return settingsService.getAll(filter)
    }

    @ApiOperation("Get a Setting.")
    @GetMapping(value = ["/api/v1/settings/{name:.+}"])
    operator fun get(@ApiParam("Name of the Setting.") @PathVariable name: String): Setting {
        return settingsService.get(name)
    }

    @ApiOperation("Update Settings.")
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).DEV) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/settings"])
    fun set(@ApiParam("Settings to update.") @RequestBody settings: Map<String, String>): Any {
        val count = settingsService.setAll(settings)
        return HttpUtils.status("settings", "update", count == settings.size)
    }
}
