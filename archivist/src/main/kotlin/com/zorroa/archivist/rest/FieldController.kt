package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class FieldController @Autowired constructor(
        val fieldSystemService: FieldSystemService
) {

    @PostMapping(value = ["/api/v1/fields"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: FieldSpec): Field {
        return fieldSystemService.createField(spec)
    }

    @GetMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun get(@PathVariable id: UUID): Field {
        return fieldSystemService.getField(id)
    }

    @PutMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun update(@RequestBody spec: FieldUpdateSpec, @PathVariable id: UUID): Any {
        val field = fieldSystemService.getField(id)
        val updated = fieldSystemService.updateField(field, spec)
        return HttpUtils.updated("field", id, updated, fieldSystemService.getField(id))
    }

    @DeleteMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun delete(@PathVariable id: UUID): Any {
        return  HttpUtils.status("field", id, "delete",
                fieldSystemService.deleteField(fieldSystemService.getField(id)))
    }

    @PostMapping(value = ["/api/v1/fields/_search"])
    @Throws(Exception::class)
    fun search(@RequestBody filter: FieldFilter): KPagedList<Field> {
        return fieldSystemService.getAllFields(filter)
    }
}