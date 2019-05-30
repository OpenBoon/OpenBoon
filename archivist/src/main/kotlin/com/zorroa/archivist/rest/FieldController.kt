package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpecCustom
import com.zorroa.archivist.domain.FieldSpecExpose
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FieldController @Autowired constructor(
    val fieldSystemService: FieldSystemService
) {

    @PostMapping(value = ["/api/v1/fields"])
    @Throws(Exception::class)
    fun create(@RequestBody spec: FieldSpecCustom): Field {
        return fieldSystemService.createField(spec)
    }

    @PostMapping(value = ["/api/v1/fields/_expose"])
    @Throws(Exception::class)
    fun expose(@RequestBody spec: FieldSpecExpose): Field {
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
        return HttpUtils.status("field", id, "delete",
                fieldSystemService.deleteField(fieldSystemService.getField(id)))
    }

    @RequestMapping(value = ["/api/v1/fields/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    @Throws(Exception::class)
    fun search(
        @RequestBody(required = false) req: FieldFilter?,
        @RequestParam(value = "from", required = false) from: Int?,
        @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<Field> {
        val filter = req ?: FieldFilter()
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return fieldSystemService.getAllFields(filter)
    }

    @RequestMapping(value = ["/api/v1/fields/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    @Throws(Exception::class)
    fun findOne(@RequestBody(required = false) req: FieldFilter?): Field {
        return fieldSystemService.findOneField(req ?: FieldFilter())
    }
}