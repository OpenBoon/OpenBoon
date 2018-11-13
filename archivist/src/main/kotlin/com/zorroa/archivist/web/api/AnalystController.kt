package com.zorroa.archivist.web.api

import com.zorroa.archivist.service.AnalystService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController


@RestController
class AnalystController @Autowired constructor(
        val analystService: AnalystService) {

}
