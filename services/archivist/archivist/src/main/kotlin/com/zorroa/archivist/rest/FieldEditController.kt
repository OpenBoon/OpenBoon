package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FieldEdit
import com.zorroa.archivist.domain.FieldEditFilter
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.service.AssetService
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Api(tags = ["Field Edit"], description = "Operations for interacting with Field Edits.")
class FieldEditController @Autowired constructor(
    val fieldSystemService: FieldSystemService,
    val assetService: AssetService
) {

    @ApiOperation("Get a Field Edit.")
    @GetMapping(value = ["/api/v1/fieldEdits/{id}"])
    @Throws(Exception::class)
    fun get(@ApiParam("UUID of the Field Edit.") @PathVariable id: UUID): FieldEdit {
        return fieldSystemService.getFieldEdit(id)
    }

    @ApiOperation("Create a Field Edit.")
    @PostMapping(value = ["/api/v1/fieldEdits"])
    @PreAuthorize(
        "hasAuthority(T(com.zorroa.security.Groups).WRITE) or " +
        "hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    fun create(@ApiParam("Field Edit to create.") @RequestBody spec: FieldEditSpec): FieldEdit {
        return assetService.createFieldEdit(spec)
    }

    @ApiOperation("Delete a Field Edit.")
    @DeleteMapping(value = ["/api/v1/fieldEdits/{id}"])
    @PreAuthorize(
        "hasAuthority(T(com.zorroa.security.Groups).WRITE) or " +
            "hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    fun delete(@ApiParam("UUID of the Field Edit.") @PathVariable id: UUID): Any {
        val edit = fieldSystemService.getFieldEdit(id)
        return HttpUtils.deleted("fieldEdit", id, assetService.deleteFieldEdit(edit))
    }

    @ApiOperation("Search for Field Edits.")
    @PostMapping(value = ["/api/v1/fieldEdits/_search"])
    fun search(@ApiParam("Search filter.") @RequestBody(required = false) filter: FieldEditFilter?): KPagedList<FieldEdit> {
        return fieldSystemService.getFieldEdits(filter ?: FieldEditFilter())
    }
}
