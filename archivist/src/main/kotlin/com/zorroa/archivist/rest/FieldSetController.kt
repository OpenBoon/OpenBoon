package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.FieldSet
import com.zorroa.archivist.domain.FieldSetFilter
import com.zorroa.archivist.domain.FieldSetSpec
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.common.repository.KPagedList
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@Api(tags = ["Field Set"], description = "CRUD operations for Field Sets.")
class FieldSetController @Autowired constructor(
    val fieldSystemService: FieldSystemService
) {

    @ApiOperation("Create a Field Set.")
    @PostMapping(value = ["/api/v1/fieldSets"])
    @Throws(Exception::class)
    fun create(@ApiParam("Field Set to create.") @RequestBody spec: FieldSetSpec): FieldSet {
        return fieldSystemService.createFieldSet(spec)
    }

    @ApiOperation("Get a Field Set.")
    @GetMapping(value = ["/api/v1/fieldSets/{id}"])
    @Throws(Exception::class)
    fun get(@ApiParam("UUID of the Field Set") @PathVariable id: UUID): FieldSet {
        return fieldSystemService.getFieldSet(id)
    }

    @ApiOperation("Search for Field Sets.")
    @PostMapping(value = ["/api/v1/fieldSets/_search"])
    @Throws(Exception::class)
    fun search(@ApiParam("Search filter.") @RequestBody filter: FieldSetFilter): KPagedList<FieldSet> {
        return fieldSystemService.getAllFieldSets(filter)
    }

    @ApiOperation("Searches for a single Field Set.",
        notes = "Throws an error if more than 1 result is returned based on the given filter.")
    @PostMapping(value = ["/api/v1/fieldSets/_findOne"])
    @Throws(Exception::class)
    fun findOne(@ApiParam("Search filter.") @RequestBody filter: FieldSetFilter): FieldSet {
        return fieldSystemService.findOneFieldSet(filter)
    }
}
