package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FieldSet
import com.zorroa.archivist.domain.FieldSetFilter
import com.zorroa.archivist.domain.FieldSetSpec
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class FieldSetController @Autowired constructor(
        val fieldSystemService: FieldSystemService
) {

    @PostMapping(value = ["/api/v1/fieldSets"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: FieldSetSpec): FieldSet {
        return fieldSystemService.createFieldSet(spec)
    }

    @GetMapping(value = ["/api/v1/fieldSets/{id}"])
    @Throws(Exception::class)
    fun get(@PathVariable id: UUID): FieldSet {
        return fieldSystemService.getFieldSet(id)
    }

    @PostMapping(value = ["/api/v1/fieldSets/_search"])
    @Throws(Exception::class)
    fun search(@RequestBody filter: FieldSetFilter): KPagedList<FieldSet> {
        return fieldSystemService.getAllFieldSets(filter)
    }
}