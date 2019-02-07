package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.service.FieldSystemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class FieldEditController @Autowired constructor(
        val fieldSystemService: FieldSystemService
) {

    @GetMapping(value = ["/api/v1/fieldEdit/{id}"])
    @Throws(Exception::class)
    fun get(@PathVariable id: UUID): FieldEdit {
        return fieldSystemService.getFieldEdit(id)
    }
}