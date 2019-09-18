package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpecCustom
import com.zorroa.archivist.domain.FieldSpecExpose
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.service.FieldSystemService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.common.repository.KPagedList
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
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
@Api(tags = ["Field"], description = "Operations for interacting with Fields.")
class FieldController @Autowired constructor(
    val fieldSystemService: FieldSystemService
) {

    @ApiOperation(value = "Create a Field.")
    @PostMapping(value = ["/api/v1/fields"])
    @Throws(Exception::class)
    fun create(@ApiParam(value = "Field to create.") @RequestBody spec: FieldSpecCustom): Field {
        return fieldSystemService.createField(spec)
    }

    @ApiOperation("Expose and existing Elasticsearch field.")
    @PostMapping(value = ["/api/v1/fields/_expose"])
    @Throws(Exception::class)
    fun expose(@RequestBody spec: FieldSpecExpose): Field {
        return fieldSystemService.createField(spec)
    }

    @ApiOperation(value = "Get a Field.")
    @GetMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun get(@ApiParam(value = "UUID of the Field.") @PathVariable id: UUID): Field {
        return fieldSystemService.getField(id)
    }

    @ApiOperation(value = "Update a Field.")
    @PutMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun update(
        @ApiParam(value = "Field update.") @RequestBody spec: FieldUpdateSpec,
        @ApiParam(value = "UUID of the Field.") @PathVariable id: UUID
    ): Any {
        val field = fieldSystemService.getField(id)
        val updated = fieldSystemService.updateField(field, spec)
        return HttpUtils.updated("field", id, updated, fieldSystemService.getField(id))
    }

    @ApiOperation(value = "Delete a Field.")
    @DeleteMapping(value = ["/api/v1/fields/{id}"])
    @Throws(Exception::class)
    fun delete(@ApiParam(value = "UUID of the Field.") @PathVariable id: UUID): Any {
        return HttpUtils.status(
            "field", id, "delete",
            fieldSystemService.deleteField(fieldSystemService.getField(id))
        )
    }

    @ApiOperation(
        value = "Search for Fields.",
        notes = "Returns a list of Fields that match the given search filter."
    )
    @RequestMapping(value = ["/api/v1/fields/_search"], method = [RequestMethod.GET, RequestMethod.POST])
    @Throws(Exception::class)
    fun search(
        @ApiParam(value = "Search filter.") @RequestBody(required = false) req: FieldFilter?,
        @ApiParam(value = "Result number to start from.") @RequestParam(value = "from", required = false) from: Int?,
        @ApiParam(value = "Number of results per page..") @RequestParam(value = "count", required = false) count: Int?
    ): KPagedList<Field> {
        val filter = req ?: FieldFilter()
        from?.let { filter.page.from = it }
        count?.let { filter.page.size = it }
        return fieldSystemService.getAllFields(filter)
    }

    @ApiOperation(
        value = "Searches for a single Field",
        notes = "Throws an error if more than 1 result is returned based on the given filter."
    )
    @RequestMapping(value = ["/api/v1/fields/_findOne"], method = [RequestMethod.GET, RequestMethod.POST])
    @Throws(Exception::class)
    fun findOne(@ApiParam(value = "Search filter.") @RequestBody(required = false) req: FieldFilter?): Field {
        return fieldSystemService.findOneField(req ?: FieldFilter())
    }

    @ApiOperation(value = "Get all possible ES attribute types.")
    @GetMapping(value = ["/api/v1/fields/_attrTypes"])
    @Throws(Exception::class)
    fun fieldAttrTypes(): List<String> {
        return AttrType.values().filter { it.name != "StringSuggest" }.map { it.toString() }
    }
}