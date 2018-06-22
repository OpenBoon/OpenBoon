package com.zorroa.irm.studio.controller

import com.zorroa.common.domain.IndexRoute
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

/**
 * A very basic ES index route lookup controller for testing and future on-prem use.
 */
@RestController
class OnPremIndexRouteController {

    @GetMapping("/api/v1/index-routes/{id}")
    fun find(@PathVariable id:String) : IndexRoute {
        return IndexRoute(UUID.fromString(id),
                "100",
                "http://10.128.0.8:9200",
                "zorroa_v10",
                null)
    }
}
