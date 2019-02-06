package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.*
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class FieldSystemController @Autowired constructor(
        val fieldSystemService: FieldSystemService
) {

    @PostMapping(value = ["/api/v1/fields"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: FieldSpec): Field {
        return fieldSystemService.create(spec)
    }

    @GetMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun get(@PathVariable id: UUID): Field {
        return fieldSystemService.get(id)
    }

    @PostMapping(value = ["/api/v1/fields/_search"])
    @Throws(Exception::class)
    fun search(@RequestBody filter: FieldFilter): KPagedList<Field> {
        return fieldSystemService.getAll(filter)
    }
}