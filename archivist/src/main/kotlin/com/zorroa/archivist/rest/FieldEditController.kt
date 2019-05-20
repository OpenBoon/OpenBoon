package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldEditFilter
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FieldEditController @Autowired constructor(
    val fieldSystemService: FieldSystemService,
    val assetService: AssetService
) {

    @GetMapping(value = ["/api/v1/fieldEdits/{id}"])
    @Throws(Exception::class)
    fun get(@PathVariable id: UUID): FieldEdit {
        return fieldSystemService.getFieldEdit(id)
    }

    @PostMapping(value = ["/api/v1/fieldEdits"])
    fun create(@RequestBody spec: FieldEditSpec): FieldEdit {
        return assetService.createFieldEdit(spec)
    }

    @DeleteMapping(value = ["/api/v1/fieldEdits/{id}"])
    fun delete(@PathVariable id: UUID): Any {
        val edit = fieldSystemService.getFieldEdit(id)
        return HttpUtils.deleted("fieldEdit", id, assetService.deleteFieldEdit(edit))
    }

    @PostMapping(value = ["/api/v1/fieldEdits/{id}"])
    fun search(@RequestBody(required = false) filter: FieldEditFilter?): KPagedList<FieldEdit> {
        return fieldSystemService.getFieldEdits(filter ?: FieldEditFilter())
    }
}