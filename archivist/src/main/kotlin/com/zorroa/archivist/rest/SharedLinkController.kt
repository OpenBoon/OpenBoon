package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.SharedLink
import com.zorroa.archivist.domain.SharedLinkSpec
import com.zorroa.archivist.service.SharedLinkService
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@RestController
@Timed
class SharedLinkController @Autowired constructor(
    private val sharedLinkService: SharedLinkService
) {

    @PostMapping(value = ["/api/v1/shared_link"])
    fun create(@Valid @RequestBody spec: SharedLinkSpec): SharedLink {
        return sharedLinkService.create(spec)
    }

    @GetMapping(value = ["/api/v1/shared_link/{id}"])
    fun get(@PathVariable id: UUID): SharedLink {
        return sharedLinkService.get(id)
    }
}
