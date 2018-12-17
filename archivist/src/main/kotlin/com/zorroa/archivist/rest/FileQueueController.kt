package com.zorroa.archivist.rest

import com.zorroa.archivist.repository.FileQueueDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class FileQueueController @Autowired constructor(
        private val fileQueueDao: FileQueueDao
) {

    /**
     * The _meters endpoint exposes company names.
     */
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).SUPERADMIN)")
    @GetMapping(value = ["/api/v1/file-queue/_meters"])
    fun getOrganizationMeters(): Any {
        return fileQueueDao.getOrganizationMeters()
    }
}