package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.DataSet
import com.zorroa.archivist.domain.DataSetSpec
import com.zorroa.archivist.service.DataSetService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DataSetController(
    val datasetService: DataSetService
) {

    @ApiOperation("Create a DataSet")
    @PostMapping("/api/v1/data-sets")
    fun create(@ApiParam("Create a new DataSet") @RequestBody spec: DataSetSpec): DataSet {
        return datasetService.create(spec)
    }

    @ApiOperation("Get a DataSet")
    @GetMapping("/api/v1/data-sets/{id}")
    fun get(@ApiParam("Get a dataset by ID") @PathVariable id: UUID): DataSet {
        return datasetService.get(id)
    }
}
