package com.zorroa.archivist.domain

import com.google.common.collect.ImmutableSet
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Settings Filter", description = "Search filter for finding settings.")
class SettingsFilter {

    @ApiModelProperty("If true only return \"live settings\", which are settings that can be modified.")
    var liveOnly: Boolean = false

    @ApiModelProperty("Set of exact setting names to return.")
    var names: Set<String> = ImmutableSet.of()

    @ApiModelProperty("Set of startsWith filters to match setting names against.")
    var startsWith: Set<String> = ImmutableSet.of()

    @ApiModelProperty("Maximum number of settings to return.")
    var count = Integer.MAX_VALUE

    fun matches(setting: Setting): Boolean {
        if (names?.isNotEmpty()) {
            if (!names.contains(setting.name)) {
                return false
            }
        }
        if (startsWith?.isNotEmpty()) {
            var match = false
            for (prefix in startsWith) {
                if (setting.name.startsWith(prefix)) {
                    match = true
                    break
                }
            }
            if (!match) {
                return false
            }
        }

        if (liveOnly) {
            if (!setting.isLive) {
                return false
            }
        }

        return true
    }
}
