package com.zorroa.archivist.web.api

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.service.SharedLinkService
import com.zorroa.archivist.web.InvalidObjectException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import javax.validation.Valid

@RestController
class SharedLinkController @Autowired constructor(
        private val sharedLinkService: SharedLinkService
){

    @PostMapping(value = "/api/v1/shared_link")
    fun create(@Valid @RequestBody spec: SharedLinkSpec, valid: BindingResult): SharedLink {
        if (valid.hasErrors()) {
            throw InvalidObjectException("Failed to create shared link", valid)
        }
        return sharedLinkService!!.create(spec)
    }

    @GetMapping(value = "/api/v1/shared_link/{id}")
    fun create(@PathVariable id: Int): SharedLink {
        return sharedLinkService!![id]
    }
}
