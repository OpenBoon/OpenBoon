package com.zorroa.archivist.web.api

import com.zorroa.archivist.service.AnalystService
import com.zorroa.common.domain.AnalystSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/*
There are 2 controllers here, one for the client side, one of the cluster side.
 */

@RestController
class AnalystController @Autowired constructor(
        val analystService: AnalystService) {

}

@RestController
class AnalystClusterController @Autowired constructor(
        val analystService: AnalystService) {

    @PostMapping(value = ["/cluster/analysts"])
    fun ping(@RequestBody spec: AnalystSpec) : Any {
        return analystService.upsert(spec)
    }
}