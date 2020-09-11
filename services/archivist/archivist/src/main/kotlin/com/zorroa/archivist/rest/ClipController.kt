package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.CreateTimelineResponse
import com.zorroa.archivist.domain.TimelineSpec
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.ClipService
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
@Api(tags = ["Timeline"], description = "Operations for interacting with Timelines")
class ClipController @Autowired constructor(
    val assetService: AssetService,
    val clipService: ClipService
) {

    @PreAuthorize("hasAuthority('AssetsRead')")
    @RequestMapping("/api/v1/clips/_timeline", method = [RequestMethod.POST])
    fun create(
        @RequestBody(required = true) timeline: TimelineSpec
    ): CreateTimelineResponse {
        return clipService.createClips(timeline)
    }
}
